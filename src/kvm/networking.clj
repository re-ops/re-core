(ns kvm.networking
  (:require
   [re-core.specs :refer (ip?)]
   [clojure.string :refer (split)]
   [re-share.wait :refer (wait-for)]
   [taoensso.timbre :as timbre]
   [clojure.core.strint :refer (<<)]
   [re-mote.ssh.transport :refer (execute)]
   [re-mote.log :refer (get-log collect-log)]
   [re-share.core :refer (gen-uuid)]
   [clojure.java.shell :refer (sh)]
   [clojure.data.zip.xml :as zx]
   [kvm.common :refer (connect domain-zip)]))

(timbre/refer-timbre)

(defn macs [c id]
  (let [root (domain-zip c id)]
    (map vector
         (zx/xml-> root :devices :interface :source (zx/attr :bridge))
         (zx/xml-> root :devices :interface :mac (zx/attr :address)))))

(defn grab-nat [c id node]
  (let [[nic mac] (first (macs c id))
        uuid (gen-uuid)
        code (execute (<< "arp -n -i ~{nic}") node :out-fn (collect-log uuid))]
    (when (= code 127)
      (throw (ex-info "arp is missing, please install net-tools" {})))
    (let [addresses (map (fn [line] (zipmap [:address :type :hwaddress] (split line #"\s+"))) (get-log uuid))
          mac-to-ip-match (fn [{:keys [hwaddress address]}] (and (= hwaddress mac) (ip? address)))]
      (if-let [match (first (filter mac-to-ip-match addresses))]
        (match :address)
        (do
          (debug (<< "nat ip not found for nic: ~{nic} mac: ~{mac}"))
          nil)))))

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

(defn public-ip
  [c user node id & {:keys [public-nic] :or {public-nic "eth1"}}]
  (wait-for-nat c id node [1 :minute])
  (let [uuid (gen-uuid) nat (nat-ip c id node)
        cmd (<< "ssh ~{ignore-authenticity} ~{user}@~{nat} -C 'ifconfig ~{public-nic}'")]
    (execute cmd node :out-fn (collect-log uuid))
    (let [log (or (inet-line (get-log uuid)) "")]
      (if-let [ip (second (re-matches #".*inet (\d+\.\d+\.\d+\.\d+).*" log))]
        ip
        (throw (ex-info "Failed to grab domain public IP" {:user user :node node :id id :output log}))))))
