(ns re-core.repl.fixtures
  (:require
    [clojure.core.strint :refer (<<)]
    [re-core.common :refer (slurp-edn)]))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(defn read-fixture [fixture]
  (slurp-edn (<< "data/resources/~{fixture}.edn")))

(defn ec2-instance [m]
  (merge-with merge
    (read-fixture "redis-ec2-system") {:aws {:key-name host}} m))

(defn with-ebs [spec size]
  (merge-with merge spec
    {:aws  {:volumes  [{:device "/dev/sdn" :size size :clear true :volume-type "standard" }]}}))

(def kvm-instance
  (read-fixture "redis-kvm"))
