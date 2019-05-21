(ns re-core.integration.es.common
  (:require
   [mount.core :as mount]
   [es.types :as t]
   [rubber.core :refer (list-indices delete-index)]
   [es.common :refer (types)]
   [re-share.config.core :as conf]
   [re-share.es.common :as es]
   [re-mote.persist.es :refer (elastic)]
   [es.systems :as s]))

(defn re-initlize
  "Re-init datastore"
  ([]
   (re-initlize false))
  ([c]
   (mount/start #'elastic)
   (when c
     (doseq [idx (filter #(.startsWith % "re-core") (map :index (or (list-indices) [])))]
       (delete-index idx)))
   (es/initialize :re-core types false)))

(defn stop []
  (mount/stop))

(defn populate-system
  "Adds single type and system"
  [type system id]
  (mount/stop)
  (re-initlize)
  (t/create type)
  (s/create system id))
