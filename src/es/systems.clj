(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get partial])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [es.common :refer (index)]
   [re-share.es.common :as common]
   [re-share.es.node :refer (connection)]
   [qbits.spandex :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn exists?
  [id]
  (common/exists? index :system id))

(defn create
  "create a system returning its id"
  ([system]
   (let [{:keys [status body] :as m} (s/request (connection) {:url [index :system] :method :post :body system})]
     (assert (#{201 200} status))
     (body :_id)))
  ([system id]
   (= (:status (s/request (connection) {:url [index :system id] :method :post :body system})) 200)))

(defn put
  "Update a system"
  [id system]
  (common/put index :system id system))

(defn delete
  "delete a system from ES"
  [id]
  (common/delete index :system id))

(defn keywordize
  "converting ES values back into keywords"
  [m]
  (when m
    (transform
     [(multi-path [:type] [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword m)))

(defn get
  "Grabs a system by an id, return nil if missing"
  [id]
  (keywordize (common/get index :system id)))

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
    (= (:status (s/request (connection) {:url [index :system id] :method :put :body (merge-with merge system part)})) 200)))

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
  (:body
   (s/request  {:url [index :_search] :method :get :body {:from from :size size :query query}})))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get (spec :system-id)) ks))

(defn all
  "return all existing systems"
  []
  (mapv (fn [[k v]] [k (keywordize v)]) (common/all :system)))
