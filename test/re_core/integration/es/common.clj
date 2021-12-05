(ns re-core.integration.es.common
  (:require
   [re-core.persistency.xtdb :as xtdb]
   [re-core.persistency.types :as t]
   [re-core.persistency.systems :as s]
   [mount.core :as mount]))

(defn stop []
  (mount/stop))

(defn populate-system
  "Adds single type and system"
  [type system id]
  (t/create type)
  (s/create system id))
