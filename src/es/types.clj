(ns es.types
  "Types persistency"
  (:refer-clojure :exclude [get partial type])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [qbits.spandex :as s]
   [es.common :refer (index)]
   [rubber.core :as z]
   [rubber.node :refer (connection)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn exists?
  [id]
  (z/exists? (index :type) :type id))

(defn create
  "create a type with id type"
  ([type]
   (try
     (= (:status (s/request (connection) {:url [(index :type) :type (type :type)] :method :post :body (dissoc type :type)})) 200)
     (catch Exception e
       (error e
              (ex-data e)
              (throw e))))))

(defn put
  "Update a type"
  [type]
  (z/put (index :type) :type (type :type) type))

(defn delete
  "delete a type from ES"
  [t]
  (z/delete (index :type) :type t))

(defn get
  "Grabs a type by its name"
  [t]
  (z/get (index :type) :type t))

(defn get!
  "Grabs a type by an id"
  [t]
  (if-let [result (get t)]
    result
    (throw (ex-info "Missing type" {:type t}))))

(defn partial
  "partial update of a type into Elasticsearch"
  [t part]
  (let [type (get t)]
    (= (:status (s/request (connection) {:url [(index :type) t] :method :put :body (merge-with merge type part)})) 200)))

(defn all
  "return all existing types"
  []
  (z/all (index :type)))

(defn clear []
  (z/delete-all (index :type)))

