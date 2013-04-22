(ns proxmox.generators
  "proxomx generated valudes"
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]
    [proxmox.remote :only (prox-get)]
    [celestial.common :only (get* import-logging)]
    [clojure.java.data :only (from-java)]
    [celestial.persistency :only (defgen)])
  (:import 
    org.nmap4j.Nmap4j 
    org.nmap4j.parser.OnePassParser
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
        (debug id)
        (if-not (ct-exists id)
          id
          (recur (- i 1)))))))

(defn ct-id
  "Generates a container id" 
  [_]
   (if-let [gid (try-gen)]
      gid
     (throw+ {:type :id-gen-error} "Failed to generate id for container")))


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

; (clojure.pprint/pprint (hosts-scan "192.168.20.64"))


