(ns re-core.features.common
  "Common testing workflows fns"
  (:require
   [es.systems :as s]
   ; setup
   [mount.core :as mount]
   [re-core.queue :refer (queue)]
   [re-core.workers :refer (workers)]
   [re-core.schedule :refer (schedule)]
   [re-share.config :as conf]))

(defn spec
  ([] (spec {}))
  ([m] (assoc (merge-with merge (s/get "1") m) :system-id "1")))

(defn get-spec [& ks]
  (get-in (spec) ks))

(defn setup
  "Fixtures setup for provisioning tests"
  [f]
  (conf/load (fn [_] {}))
  (mount/start #'schedule #'queue #'workers)
  (f)
  (mount/stop))
