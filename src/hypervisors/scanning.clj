(ns hypervisors.scanning
  "Scanning hosts"
  (:require 
    [hypervisors.networking :refer (list-used-ips)]
    [clojure.data :refer [diff]]
    [clojure.java.data :refer [from-java]]) 
  (:import 
    org.nmap4j.Nmap4j 
    org.nmap4j.parser.OnePassParser))

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

(defn correlate
  "compares stored ips to scan result"
  [k]
  (let [scanned (map (fn [[{:keys [addr]}]] addr) (hosts-scan "192.168.20.170-254"))]
    (zipmap [:scanned :listed :common] (diff (into #{} scanned) (into #{} (list-used-ips k))))))
