(ns celestial.common
  (:use 
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (warn)]
    [clj-config.core :as conf]))

(def config 
  (let [path (<< "~(System/getProperty \"user.home\")/.celestial.edn")]
    (if (.exists (file path)) 
      (conf/read-config path)
      (do 
        (warn (<< "~{path} does not exist, make sure to configure it")) 
        {} 
        ) 
      )))
