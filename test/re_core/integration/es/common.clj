(ns re-core.integration.es.common
  (:require
   [es.types :as t]
   [rubber.core :refer (list-indices delete-index)]
   [re-share.components.elastic :as esc]
   [es.common :refer (types)]
   [re-share.config :as conf]
   [es.systems :as s]))

(def elastic (esc/instance types :re-core false))

(defn re-initlize
  "Re-init datastore"
  ([]
   (re-initlize false))
  ([c]
   (conf/load (fn [_] {}))
   (.start elastic)
   (when c
     (doseq [idx (filter #(.startsWith % "re-core") (map :index (or (list-indices) [])))]
       (delete-index idx)))
   (esc/initialize :re-core types false)))

(defn populate-system
  "Adds single type and system"
  [type system id]
  (.setup elastic)
  (re-initlize)
  (t/create type)
  (s/create system id))
