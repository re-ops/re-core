(ns re-core.repl.fixtures
  (:require
   [clojure.core.strint :refer (<<)]
   [re-core.persistency.types :as t]
   [re-core.common :refer (slurp-edn)]))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(defn read-fixture [fixture]
  (slurp-edn (<< "data/resources/~{fixture}.edn")))

(defn kvm-instance [t]
  (read-fixture (name t)))

(defn populae-types []
  (doseq [id (t/all)]
    (t/delete id))
  (doseq [t ["jvm-type" "redis-type" "reops-type" "restore-type"]]
    (let [r (t/create (read-fixture t))]
      (println (<< "added type ~{t} resulting in ~{r}")))))
