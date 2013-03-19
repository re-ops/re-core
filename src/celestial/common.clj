(ns celestial.common
  (:import java.util.Date)
  (:use 
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (warn)]
    [clj-config.core :as conf]))

(def path (<< "~(System/getProperty \"user.home\")/.celestial.edn"))

(defn config-exists? [] (.exists (file path)))

(def config 
   (if (config-exists?)
      (conf/read-config path)
      (do 
        (warn (<< "~{path} does not exist, make sure to configure it")) 
        {} 
        ) 
      ))


(defn slurp-edn [file] (read-string (slurp file)))

(defn import-logging []
    (use '[taoensso.timbre :only (debug info error warn trace)]))

(defn curr-time [] (.getTime (Date.)))
