(ns re-core.repl.fixtures
  (:require
   [clojure.core.strint :refer (<<)]
   [re-core.persistency.types :as t]
   [re-core.common :refer (slurp-edn)]))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(defn read-fixture [fixture]
  (slurp-edn (<< "data/resources/~{fixture}.edn")))

(defn ec2-instance [m]
  (merge-with merge
              (read-fixture "redis-ec2-system") {:aws {:key-name host}} m))

(defn with-ebs [spec size]
  (merge-with 
    merge spec {:aws  {:volumes  [{:device "/dev/sdn" :size size :clear true :volume-type "standard"}]}}))

(defn kvm-instance [t]
  (read-fixture (name t)))

(defn populae-types []
  (doseq [id (t/all-types)]
    (t/delete-type id))
  (doseq [t ["jvm-type" "redis-type" "reops-type"]]
    (let [r (t/add-type (read-fixture t))]
      (println (<< "added type ~{t} resulting in ~{r}")))))
