(ns celestial.common
  (:import java.util.Date)
  (:use 
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (warn)]
    [clj-config.core :as conf]))

(def path 
  (first (filter #(.exists (file %))
    ["/etc/celestial.edn" (<< "~(System/getProperty \"user.home\")/.celestial.edn")])))

(def config 
   (if path
      (conf/read-config path)
      (do 
        (warn (<< "~{path} does not exist, make sure to configure it using default configuration")) 
        {:celestial 
          {:log {:level :trace :path "celestial.log" :gelf-host ""} }} 
        ) 
      ))

(defn get* [& keys]
  (get-in config keys))

(defn slurp-edn [file] (read-string (slurp file)))

(defn import-logging []
    (use '[taoensso.timbre :only (debug info error warn trace)]))

(defn curr-time [] (.getTime (Date.)))
