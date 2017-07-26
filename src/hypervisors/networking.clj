(ns hypervisors.networking
  "Common hypervizors networking logic"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.sshj :refer (execute)]
   [selmer.filters :refer (add-filter!)]
   [clojure.core.strint :refer [<<]]
   [selmer.parser :refer (render-file)]
   [re-core.common :refer [get!]]
   [re-core.model :refer [hypervisor get-env! set-env]]
   [slingshot.slingshot :refer [throw+ try+]]
   [re-core.redis :refer [wcar]]
   [flatland.useful.utils :refer (defm)]
   [taoensso.carmine :as car]))

(refer-timbre)

(defn ip-to-long
  "Converts an ip address to long number"
  [^String ip]
  (let [atoms ^"[Ljava.lang.String;" (.split ip "\\.")]
    (loop [i 3 result 0]
      (if (>= i 0)
        (recur (- i 1) (bit-or result (bit-shift-left (Long/parseLong (aget atoms (- 3 i))) (* i 8))))
        (bit-and result 0xFFFFFFFF)))))

(defn add-segment
  "Adds an ip segment"
  [^StringBuilder sb ip* i]
  (.insert sb 0 (Long/toString (bit-and ip* 0xff)))
  (when (< i 3) (.insert sb 0 "."))
  sb)

(defn long-to-ip
  {:test #(assert (= "172.168.10.60" (long-to-ip (ip-to-long "172.168.10.60"))))
   :doc "Converting long to ip address"}
  [ip]
  (loop [i 0 sb (StringBuilder. 15) ip* ip]
    (if (< i 4)
      (recur (+ i 1) (add-segment sb ip* i) (bit-shift-right ip* 8))
      (.toString sb))))

(defn- ip-range
  "Configured ip range"
  [hyp]
  (try+
   (let [[s e] (map ip-to-long (hypervisor hyp :generators :ip-range))]
     [s e])
   (catch [:type :re-core.common/missing-conf] e nil)))

(defn- ips-key [hyp]
  {:pre [(keyword? hyp)]}
  (<< "~(name (get-env!)):~(name hyp):ips"))

(defn mark
  "marks a single ip as used"
  [ip hyp] {:pre [(re-find #"\d+\.\d+\.\d+\.\d+" ip)]}
  (wcar (car/zadd (ips-key hyp) 1 (ip-to-long ip))) ip)

(defn mark-conf
  "Marks all used ips in configuration"
  [hyp]
  (doseq [ip (hypervisor hyp :generators :used-ips)]
    (mark ip hyp)))

(defn initialize-range
  "Initializes ip range zset 0 marks unused 1 marks used (only if missing)."
  [hyp]
  (when-let [[s e] (ip-range hyp)]
    (let [hk (ips-key hyp)]
      (wcar
       (when-not (= 1 (car/exists hk))
         (doseq [ip (range s (+ 1 e))]
           (car/zadd hk 0 ip)))))))

(defn- fetch-ip
  "Redis ip range fetcher"
  [k]
  (some->
   (wcar
    (car/lua-script
     "local next = redis.call('zrangebyscore', _:ips,0,0, 'LIMIT', 0,1) -- next available ip
         redis.call('zadd',_:ips,_:used, next[1]) -- mark as used
         return next[1]"
     {:ips k} {:used "1"})) Long/parseLong long-to-ip))

(defn initialize-networking
  "intializes all the networking ranges and marks used ones (will not override existing ranges)"
  []
  (let [envs (flatten (map #(interleave (repeat %) (keys (get! :hypervisor %))) (keys (get! :hypervisor))))]
    (doseq [[e hyp] (partition 2 envs)]
      (set-env e
               (when (= 0 (wcar (car/exists (ips-key hyp))))
                 (initialize-range hyp))
               (when (get-in (hypervisor hyp) [:generators :used-ips])
           ; in case somthing was added (does not free ips)
                 (mark-conf hyp))))))

(defn gen-ip
  "Associates an available ip address (into m -> target) from range, fails if range is exhausted."
  [m hyp target] {:pre [(keyword? hyp)]}
  (let [hk (ips-key hyp)]
    (if-let [ip (fetch-ip hk)]
      (assoc m target ip)
      (throw+ {:type ::ip-gen-error} (<< "Failed to obtain ip for ~{hyp}")))))

(defn release-ip
  [ip hyp]
  (wcar
   (when ip
     (car/lua-script
      "if redis.call('zrank', _:ips, _:rel-ip) then
        redis.call('zadd',_:ips, 0, _:rel-ip)
        return _:rel-ip
        end
        return nil "
      {:ips (ips-key hyp)} {:rel-ip (ip-to-long ip)}))))

; debugging fn's
(defn list-used-ips
  "List used ips in human readable form (mainly for debugging)."
  [hyp]
  (map #(-> % (Long/parseLong) long-to-ip) (wcar (car/zrangebyscore (ips-key hyp) 1 1))))

(defn list-free-ips
  "List free ips in human readable form (mainly for debugging)."
  [hyp]
  (map #(-> % (Long/parseLong) long-to-ip) (wcar (car/zrangebyscore (ips-key hyp) 0 0))))

(defn clear-range
  "mainly for testing"
  [hyp] (wcar (car/del (ips-key hyp))))

(test #'long-to-ip)

(add-filter! :not-empty? (comp not empty?))

(defn debian-interfaces
  "Generates a static ip template"
  [config]
  (render-file "interfaces.slem" config))

(defn redhat-network-cfg [config]
  (render-file "network.slem" config))

(defn redhat-ifcfg-eth0 [config]
  (render-file "ifcfg-eth0.slem" config))

(defn override-hostname
  "sets hostname and hosts file"
  [hostname fqdn remote]
  (execute (<< "echo ~{hostname} | sudo tee /etc/hostname") remote)
  (execute (<< "echo 127.0.1.1 ~{fqdn} ~{hostname} | sudo tee -a /etc/hosts") remote))

(defn kernel-hostname
  "Set hosname in kernel for all OSes"
  [hostname fqdn remote]
  (execute (<< "echo kernel.hostname=~{hostname} | sudo tee -a /etc/sysctl.conf") remote)
  (execute (<< "echo kernel.domainname=\"~{fqdn}\" | sudo tee -a /etc/sysctl.conf") remote)
  (execute "sudo sysctl -e -p" remote))

(defn redhat-hostname
  "Sets up hostname under /etc/sysconfig/network in redhat based systems"
  [fqdn remote]
  (execute
   (<< "grep -q '^HOSTNAME=' /etc/sysconfig/network && sudo sed -i 's/^HOSTNAME=.*/HOSTNAME=~{fqdn}' /etc/sysconfig/network || sudo sed -i '$ a\\HOSTNAME=~{fqdn}' /etc/sysconfig/network") remote))

(defn set-hostname
  [hostname fqdn remote flavor]
  (kernel-hostname hostname fqdn remote)
  (override-hostname hostname fqdn remote)
  (case flavor
    :debian  true ; nothing special todo
    :redhat  (redhat-hostname fqdn remote)
    (throw+ {:type ::no-matching-flavor} (<< "no os flavor found for ~{flavor}"))))
