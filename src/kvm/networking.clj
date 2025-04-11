(ns kvm.networking
  (:require
   [re-core.specs :refer (ip?)]
   [clojure.string :refer (split split-lines)]
   [re-share.wait :refer (wait-for)]
   [taoensso.timbre :as timbre]
   [clojure.core.strint :refer (<<)]
   [re-mote.ssh.transport :refer (execute)]
   [re-mote.log :refer (get-log collect-log)]
   [re-share.core :refer (gen-uuid)]
   [clojure.java.shell :refer (sh)]
   [clojure.data.zip.xml :as zx]
   [kvm.common :refer (domain-zip)]))

(timbre/refer-timbre)

(defn- assert-code [code]
  (when (= code 127)
    (throw (ex-info "arp is missing, please install net-tools" {}))))

(defn- run-arp [node nic]
  (if (= (:host node) "localhost")
    (let [{:keys [exit out]} (sh "arp" "-n" "-i" nic)]
      (assert-code exit)
      (split-lines out))
    (let [uuid (gen-uuid)
          code (execute (<< "arp -n -i ~{nic}") node :out-fn (collect-log uuid))]
      (assert-code code)
      (get-log uuid))))

(defn bridges [c id]
  "Extract bridge interfaces from domain XML"
  (let [root (domain-zip c id)]
    (map (fn [bridge mac target-dev network portid]
           {:type "network"
            :bridge bridge
            :mac mac
            :target-dev target-dev
            :network network
            :portid portid})
         (zx/xml-> root :devices :interface (zx/attr= :type "network") :source (zx/attr :bridge))
         (zx/xml-> root :devices :interface (zx/attr= :type "network") :mac (zx/attr :address))
         (zx/xml-> root :devices :interface (zx/attr= :type "network") :target (zx/attr :dev))
         (zx/xml-> root :devices :interface (zx/attr= :type "network") :source (zx/attr :network))
         (zx/xml-> root :devices :interface (zx/attr= :type "network") :source (zx/attr :portid)))))

(defn macvtaps [c id]
  "Extract macvtap interfaces from domain XML"
  (let [root (domain-zip c id)]
    (map (fn [device mac target-dev mode]
           {:type "direct"
            :device device
            :mac mac
            :target-dev target-dev
            :mode mode})
         (zx/xml-> root :devices :interface (zx/attr= :type "direct") :source (zx/attr :dev))
         (zx/xml-> root :devices :interface (zx/attr= :type "direct") :mac (zx/attr :address))
         (zx/xml-> root :devices :interface (zx/attr= :type "direct") :target (zx/attr :dev))
         (zx/xml-> root :devices :interface (zx/attr= :type "direct") :source (zx/attr :mode)))))

(defn grab-nat [c id node]
  (let [{:keys [bridge mac]} (first (bridges c id))
        out (run-arp node bridge)]
    (let [addresses (map (fn [line] (zipmap [:address :type :hwaddress] (split line #"\s+"))) out)
          mac-to-ip-match (fn [{:keys [hwaddress address]}] (and (= hwaddress mac) (ip? address)))]
      (if-let [match (first (filter mac-to-ip-match addresses))]
        (match :address)
        (do
          (debug (<< "nat ip not found for target device: ~{bridge} mac: ~{mac}")) nil)))))

(defn wait-for-nat
  "Waiting for nat cache to update"
  [c id node timeout]
  (wait-for {:timeout timeout :sleep [2000 :ms]}
            (fn [] (not (nil? (grab-nat c id node))))
            "Timed out on waiting for arp cache to update"))

(defn nat-ip [c id node]
  (wait-for-nat c id node [1 :minute])
  (let [ip (grab-nat c id node)]
    (debug (<< "found nat ip ~{ip} for ~{id}"))
    ip))

(def ignore-authenticity "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no")

(defn inet-line
  [lines]
  (first (filter #(.contains % "inet") lines)))

(defn grab-public [user nat public-nic node]
  (let [uuid (gen-uuid)
        cmd (<< "ssh ~{ignore-authenticity} ~{user}@~{nat} -C 'ifconfig ~{public-nic}'")]
    (execute cmd node :out-fn (collect-log uuid))
    (let [log (or (inet-line (get-log uuid)) "")]
      (when-let [ip (second (re-matches #".*inet (\d+\.\d+\.\d+\.\d+).*" log))]
        ip))))

(defn wait-for-public
  "Waiting for public ip"
  [user nat public-nic node timeout]
  (wait-for {:timeout timeout :sleep [2000 :ms]}
            (fn [] (not (nil? (grab-public user nat public-nic node))))
            "Timed out on waiting for arp cache to update"))

(defn public-ip
  [c user node id & {:keys [public-nic] :or {public-nic "eth1"}}]
  (wait-for-nat c id node [1 :minute])
  (let [nat (nat-ip c id node)]
    (wait-for-public user nat public-nic node [30 :second])
    (grab-public user nat public-nic node)))

