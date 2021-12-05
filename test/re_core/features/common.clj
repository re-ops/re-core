(ns re-core.features.common
  "Common testing workflows fns"
  (:require
   [re-core.persistency.systems :as s]
   ; setup
   [re-ops.config.core :as conf]
   [mount.core :as mount]
   [re-core.queue :refer (queue)]
   [re-core.workers :refer (workers)]
   [re-core.persistency.xtdb :as xtdb]))

(defn spec
  ([] (spec {}))
  ([m] (assoc (merge-with merge (s/get "1") m) :system-id "1")))

(defn get-spec [& ks]
  (get-in (spec) ks))

(defn setup
  "Fixtures setup for provisioning tests"
  [f]
  (conf/load-config)
  (mount/start #'xtdb/node #'queue #'workers)
  (Thread/sleep 1000)
  (f)
  (mount/stop))
