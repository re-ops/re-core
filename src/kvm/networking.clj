(ns kvm.networking
  (:require
   [re-core.persistency.systems :as s]
   [re-core.provider :refer (wait-for)]
   [slingshot.slingshot :refer (throw+)]
   [taoensso.timbre :as timbre]
   [clojure.core.strint :refer (<<)]
   [re-mote.sshj :refer (execute)]
   [re-mote.log :refer (get-log collect-log)]
   [re-core.common :refer (gen-uuid)]
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
  (let [[nic mac] (first (macs c id)) uuid (gen-uuid)]
    (execute (<< "arp -i ~{nic}") node :out-fn (collect-log uuid))
    (if-let [line (first (filter #(.contains % mac) (get-log uuid)))]
      (first (.split line "\\s"))
      (do (debug (<< "no nat ip found for ~{mac}")) nil))))

(defn wait-for-nat [c id node timeout]
  "Waiting for nat cache to update"
  (wait-for {:timeout timeout} #(not (nil? (grab-nat c id node)))
            {:type ::kvm:networking :timeout timeout}
            "Timed out on waiting for arp cache to update"))

(defn nat-ip [c id node]
  (wait-for-nat c id node [5 :minute])
  (grab-nat c id node))

(def ignore-authenticity "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no")

(defn inet-line
  [lines]
  (first (filter #(.contains % "inet addr") lines)))

(defn public-ip
  [c user node id & {:keys [public-nic] :or {public-nic "eth1"}}]
  (let [uuid (gen-uuid) nat (nat-ip c id node)
        cmd (<< "ssh ~{ignore-authenticity} ~{user}@~{nat} -C 'ifconfig ~{public-nic}'")]
    (execute cmd node :out-fn (collect-log uuid))
    (if-let [ip (second (re-matches #".*addr\:(\d+\.\d+\.\d+\.\d+).*" (or (inet-line (get-log uuid)) "")))]
      ip
      (throw+ {:type ::kvm:networking} "Failed to grab domain public IP"))))

(defn update-ip
  "updates public dns in the machine persisted data"
  [system-id ip]
  (when (s/system-exists? system-id)
    (s/partial-system system-id {:machine {:ip ip}})))
