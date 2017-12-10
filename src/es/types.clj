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

(defn exists?
  [id]
  (= (:status (s/request @c {:url [:type id] :method :head})) 200))

(defn create
  "create a type returning its id"
  ([type]
   (= (:status (s/request @c {:url [:type] :method :post :body type})) 200))
  ([type id]
   (= (:status (s/request @c {:url [:type id] :method :post :body type})) 200)))

(defn put
  "Update a type"
  [id type]
  (= (:status (s/request @c {:url [:type id] :method :put :body type})) 200))

(defn delete
  "delete a type from ES"
  [id]
  (= (:status (s/request @c {:url [:type id] :method :delete})) 200))

(defn keywordize
  "converting ES values back into keywords"
  [m]
  (transform
   [(multi-path [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword  m))

(defn get
  "Grabs a type by an id"
  [id]
  (keywordize (:_source (s/request @c {:url [:type id] :method :get}))))

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
    (= (:status (s/request @c {:url [:type id] :method :put :body (merge-with merge type part)})) 200)))



(defn all
  "return all existing types"
  []
  [])

