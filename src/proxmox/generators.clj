(ns proxmox.generators
  "proxomx generated valudes"
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]
    [proxmox.remote :only (prox-get)]
    [celestial.common :only (get* import-logging)]
    [celestial.redis :only (wcar)]
    [clojure.java.data :only (from-java)]
    [celestial.persistency :only (defgen)])
  (:import 
    org.nmap4j.Nmap4j 
    org.nmap4j.parser.OnePassParser
    ) 
  (:require 
    [taoensso.carmine :as car] 
    )
  )

(import-logging)

(defgen ct-id)

(defn ct-exists [id]
  (try+ 
    (prox-get (<< "/nodes/proxmox/openvz/~{id}")) true
    (catch [:status 500] e false)))

(defn try-gen
  "Attempts to generate an id"
  []
  (loop [i 5]
    (when (> i 0)
      (let [id (+ 100 (gen-ct-id))]
        (if-not (ct-exists id)
          id
          (recur (- i 1)))))))

(defn ct-id
  "Generates a container id" 
  [_]
  (if-let [gid (try-gen)]
    gid
    (throw+ {:type ::id-gen-error} "Failed to generate id for container")))


(defn ports-open? [host]
  (not (nil? (first (filter #(= % "open") 
                            (map #(get-in % [:state :state]) (get-in host [:ports :ports])))))))

(defn raw-scan [ip]
  (let [nmap (Nmap4j. "/usr/local")]
    (doto nmap 
      (.includeHosts ip)
      (.addFlags "-n -T5 -p U:1194,T:21,22,25,53,80,110,111")
      (.execute) 
      (.getResult)
      )))


(defn hosts-scan 
  "Uses nmap so scan a list of hosts (192.168.10.1-50)"
  [hosts]
  (map :addresses  (filter ports-open? 
                           (get-in (from-java (raw-scan hosts)) [:result :hosts])))
  )

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


(def range-keys [])

(defn ip-range 
  "" 
  []
  (try+ 
    (let [[s e] (map ip-to-long (get* :hypervisor :proxmox :generators :ip-range))] 
      [s e])
    (catch [:type :celestial.common/missing-conf] e nil)))

(defn initialize-range
  "Initializes ip range zset 0 marks unused 1 marks used (only if missing)."
  []
  (if-let [[s e] (ip-range)]
    (wcar 
      (when-not (= 1 (car/exists "ips"))
        (doseq [ip (range s (+ 1 e))]
          (car/zadd "ips" 0 ip))))))


(defn fetch-ip
  "Redis ip range fetcher"
  []
  (some-> 
    (wcar 
      (car/lua-script
        "local next = redis.call('zrangebyscore', _:ips,0,0, 'LIMIT', 0,1) -- next available ip
        redis.call('zadd',_:ips,_:used, next[1]) -- mark as used
        return next[1]" 
        {:ips "ips"} {:used "1"})) Long/parseLong long-to-ip))

(defn gen-ip
  "Associates an available ip address from range, fails if range is exhausted."
  [ct] 
  (when (= 0 (wcar (car/exists "ips"))) (initialize-range))
  (if-let [ip (fetch-ip)]
    (assoc ct :ip_address ip)
    (throw+ {:type ::ip-gen-error} "Failed to obtain ip for container")))

(defn release-ip [ip]
  (wcar 
    (when ip
     (car/lua-script
      "if redis.call('zrank', 'ips', _:ip) then
        redis.call('zadd', 'ips', 0, _:ip) 
        return _:ip
       end 
       return nil "
      {:ips "ips"} {:ip (ip-to-long ip)}))))

(comment
  (release-ip "192.168.5.130") 
  (gen-ip {}) 
  (wcar (car/del "ips"))) 

(test #'long-to-ip)
