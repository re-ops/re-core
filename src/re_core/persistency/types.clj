(ns re-core.persistency.types
  (:require
   [xtdb.api :as xt]
   [re-core.persistency.xtdb :refer [node]]
   [re-core.persistency.common :refer [unflatten flatten-]]))

(defn get
  "Grabs a type by an id"
  [t]
  (unflatten (xt/pull (xt/db node) '[*] t)))

(defn exists?
  [t]
  (not (empty? (get t))))

(defn create
  "create a type"
  [type]
  (xt/await-tx node (xt/submit-tx node [[::xt/put (assoc (flatten- type) :xt/id (keyword (type :type)))]])))

(defn put
  "Update a type"
  [type]
  (xt/await-tx node (xt/submit-tx node [[::xt/put (assoc (flatten- type) :xt/id (keyword (type :type)))]])))

(defn delete
  "delete a type from ES"
  [t]
  (xt/await-tx node (xt/submit-tx node [[::xt/evict t]])))

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
    (put (merge-with merge type part))))

(defn all
  "return all existing types"
  []
  (map
   (fn [[m]] [(:xt/id m) (unflatten (dissoc m :xt/id))])
   (xt/q (xt/db node) '{:find [(pull ?type [*])] :where [[?type :cog/plan _]]})))

(comment
  (delete "disposeable"))
