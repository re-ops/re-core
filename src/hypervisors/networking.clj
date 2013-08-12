(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns hypervisors.networking
  "Common hypervizors networking logic"
  (:require 
    [selmer.filters :refer (add-filter!)]
    [clojure.core.strint :refer [<<]]
    [selmer.parser :refer (render-file)]
    [clojure.data :refer [diff]]
    [clojure.java.data :refer [from-java]] 
    [celestial.common :refer [get! import-logging]]
    [slingshot.slingshot :refer [throw+ try+]]
    [celestial.redis :refer [wcar]]
    [flatland.useful.utils :refer (defm)]
    [taoensso.carmine :as car])
  (:import 
    org.nmap4j.Nmap4j 
    org.nmap4j.parser.OnePassParser))

(import-logging)

(defn ports-open? [host]
  (not (nil? (first (filter #(= % "open") (map #(get-in % [:state :state]) (get-in host [:ports :ports])))))))

(defn raw-scan 
  "runs raw scan, require nmap bin to be symlinked to /usr/local"
  [ip]
  (let [nmap (Nmap4j. "/usr/local")]
    (doto nmap 
      (.includeHosts ip)
      (.addFlags "-n -T5 -p U:1194,T:21,22,25,53,80,110,111")
      (.execute) 
      (.getResult))))

(defn hosts-scan 
  "Uses nmap so scan a list of hosts (192.168.10.1-50)"
  [hosts]
  (map :addresses 
       (filter ports-open?  (get-in (from-java (raw-scan hosts)) [:result :hosts]))))


(defn ip-to-long 
  "Converts an ip address to long number" 
  [ip]
  (let [atoms (.split ip "\\.")]
    (loop [i 3 result 0]
      (if (>= i 0) 
        (recur (- i 1) (bit-or result (bit-shift-left (Long/parseLong (aget atoms (- 3 i))) (* i 8))))
        (bit-and result 0xFFFFFFFF)))))

(defn add-segment 
  "Adds an ip segment" 
  [sb ip* i]
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

(defn ip-range 
  "Configured ip range" 
  []
  (try+ 
    (let [[s e] (map ip-to-long (get! :hypervisor :proxmox :generators :ip-range))] 
      [s e])
    (catch [:type :celestial.common/missing-conf] e nil)))

(defn mark
   "marks a single ip as used" 
   [ip k]
   (wcar (car/zadd k 1 ip)) ip)

(defm mark-conf
  "Marks all used ips in configuration, memoized so it will be called once on each boot/usage"
  [k]
  (doseq [ip (map ip-to-long (get! :hypervisor :proxmox :generators :used-ips))]
    (mark ip k)))

(defn initialize-range
  "Initializes ip range zset 0 marks unused 1 marks used (only if missing)."
  [k]
  (when-let [[s e] (ip-range)]
    (wcar 
      (when-not (= 1 (car/exists k))
        (doseq [ip (range s (+ 1 e))]
          (car/zadd k 0 ip))))))

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

(defn- ips-key [k] (<< "~{k}:ips"))

(defn gen-ip
  "Associates an available ip address from range, fails if range is exhausted."
  [ct k] 
  (let [k* (ips-key k)]
    (when (= 0 (wcar (car/exists k*))) (initialize-range k*)) 
    (mark-conf k*) ; in case list was updated
    (if-let [ip (fetch-ip k*)]
      (assoc ct :ip_address ip)
      (throw+ {:type ::ip-gen-error} "Failed to obtain ip for container"))))

(defn release-ip [ip k]
  (wcar 
    (when ip
      (car/lua-script
        "if redis.call('zrank', _:ips, _:rel-ip) then
        redis.call('zadd',_:ips, 0, _:rel-ip) 
        return _:rel-ip
        end 
        return nil "
        {:ips (ips-key k)} {:rel-ip (ip-to-long ip)}))))

; debugging fn's
(defn list-used-ips
  "List used ips in human readable form (mainly for debugging)."
  [k]
  (map #( -> % (Long/parseLong) long-to-ip) (wcar (car/zrangebyscore (ips-key k) 1 1))))

(defn correlate
  "compares stored ips to scan result"
  [k]
  (let [scanned (map (fn [[{:keys [addr]}]] addr) (hosts-scan "192.168.20.170-254"))]
    (zipmap [:scanned :listed :common] (diff (into #{} scanned) (into #{} (list-used-ips k))))))

(test #'long-to-ip) 

(add-filter! :not-empty? (comp not empty?))

(defn debian-interfaces
  "Generates a static ip template" 
  [config]
  (render-file "insterfaces.slem" config))

(defn redhat-network [config]
  (render-file "network.slem" config))

(defn redhat-ifcfg-eth0 [config]
  (render-file "ifcfg-eth0.slem" config))
