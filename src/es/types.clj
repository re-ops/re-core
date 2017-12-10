(ns es.types
  "Types persistency"
  (:refer-clojure :exclude [get partial type])
  (:require
   [es.common :refer (index)]
   [es.node :as node :refer (ES)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.native.document :as doc]
   [re-core.model :as model]))

(refer-timbre)

(def es-type "type")

(defn exists? [id]
  (doc/present? @ES index es-type id))

(defn create
  "create a system returning its id"
  ([type]
   (:id (doc/create @ES index es-type type)))
  ([type id]
   (:id (doc/create @ES index es-type type {:id id}))))

(defn put
  "Update a type"
  [id type]
  (doc/put @ES index es-type id type))

(defn delete
  "delete a type from ES"
  [id]
  (doc/delete @ES index es-type id))

(defn get
  "Grabs a type by an id"
  [id]
  (:source (doc/get @ES index es-type id)))

(defn get!
  "Grabs a type by an id"
  [id]
  (if-let [result (get id)]
    result
    (throw (ex-info "Missing type" {:id id}))))

(defn partial
  "partial update of a type into ES"
  [id part]
  (let [type (get id)]
    (doc/put @ES index es-type id (merge-with merge type part))))

(defn all
  "return all existing types"
  []
  [])

