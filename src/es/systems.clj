(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get partial])
  (:require
   [com.rpl.specter :as s :refer [select transform ALL multi-path]]
   [es.common :refer (index)]
   [es.node :as node :refer (ES)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.native.document :as doc]
   [re-core.model :as model]))

(refer-timbre)

(defn exists? [id]
  (doc/present? @ES index "system" id))

(defn create
  "create a system returning its id"
  ([system]
   (:id (doc/create @ES index "system" system)))
  ([system id]
   (:id (doc/create @ES index "system" system {:id id}))))

(defn put
  "Update a system"
  [id system]
  (doc/put @ES index "system" id system))

(defn delete
  "delete a system from ES"
  [id]
  (doc/delete @ES index "system" id))

(defn keywordize
  "converting ES values back into keywords"
  [m]
  (transform
   [(multi-path [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword  m))

(defn get
  "Grabs a system by an id"
  [id]
  (keywordize (:source (doc/get @ES index "system" id))))

(defn get!
  "Grabs a system by an id"
  [id]
  (if-let [result (get id)]
    result
    (throw (ex-info "Missing system" {:id id}))))

(defn partial
  "partial update of a system into ES"
  [id part]
  (let [system (get id)]
    (doc/put @ES index "system" id (merge-with merge system part))))

(defn clone
  "clones an existing system"
  [id {:keys [hostname] :as spec}]
  (put
   (-> (get id)
       (assoc-in [:machine :hostname] hostname)
       (model/clone spec))))

(defn query
  "basic query string"
  [query & {:keys [from size] :or {size 100 from 0}}]
  (doc/search @ES index "system" {:from from :size size :query query :fields ["owner" "env"]}))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get (spec :system-id)) ks))

(comment
  (println @ES)
  (clojure.pprint/pprint (keywordize (get! "1")))
  (delete "1")
  (es.node/connect))
