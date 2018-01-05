(ns es.types
  "Types persistency"
  (:refer-clojure :exclude [get partial type])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [qbits.spandex :as s]
   [es.common :refer (index) :as common]
   [es.node :as node :refer (c)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn exists?
  [id]
  (common/exists? index :type id))

(defn create
  "create a type returning its id"
  ([type]
   (try
     (s/request @c {:url [index :type] :method :post :body type}) 200
     (catch Exception e
       (error e (ex-data e)
              (throw e)))))
  ([type id]
   (= (:status (s/request @c {:url [index :type id] :method :post :body type})) 200)))

(defn put
  "Update a type"
  [id type]
  (common/put index :type id type))

(defn delete
  "delete a type  from ES"
  [id]
  (common/delete index :type id))

(defn keywordize
  "converting Elasticsearch values back into keywords"
  [m]
  (when m
    (transform
     [(multi-path [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword  m)))

(defn get
  "Grabs a type by an id"
  [id]
  (keywordize (common/get index :type id)))

(defn get!
  "Grabs a type by an id"
  [id]
  (if-let [result (get id)]
    result
    (throw (ex-info "Missing type" {:id id}))))

(defn partial
  "partial update of a type into Elasticsearch"
  [id part]
  (let [type (get id)]
    (= (:status (s/request @c {:url [index :type id] :method :put :body (merge-with merge type part)})) 200)))

(defn all
  "return all existing types"
  []
  (map second (common/all :type)))

(defn clear []
  (common/delete-all index :type))
