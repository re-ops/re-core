(ns es.types
  "Types persistency"
  (:refer-clojure :exclude [get partial type])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [qbits.spandex :as s]
   [es.common :refer (index)]
   [es.node :as node :refer (c)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn exists?
  [id]
  (= (:status (s/request @c {:url [index :type id] :method :head})) 200))

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
  (= (:status (s/request @c {:url [index :type id] :method :put :body type})) 200))

(defn delete
  "delete a type from Elasticsearch"
  [id]
  (= (:status (s/request @c {:url [index :type id] :method :delete})) 200))

(defn keywordize
  "converting Elasticsearch values back into keywords"
  [m]
  (transform
   [(multi-path [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword  m))

(defn get
  "Grabs a type by an id"
  [id]
  (keywordize (:_source (s/request @c {:url [index :type id] :method :get}))))

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
  [])

