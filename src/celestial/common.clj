(ns celestial.common
  (:use 
    clojure.core.strint
    [clj-config.core :as conf]))

(def config (conf/read-config (<<  "~(System/getProperty \"user.home\")/.multistage.edn")))
