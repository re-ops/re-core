(ns re-core.repl.fixtures
  (:require 
    [clojure.core.strint :refer (<<)]
    [re-core.common :refer (slurp-edn)]))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(defn read-fixture [fixture]
  (slurp-edn (<< "data/resources/~{fixture}.edn")))

(defn instance [] 
  (clojure.edn/read-string (slurp "data/resources/redis-ec2-system.edn")))
