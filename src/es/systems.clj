(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get partial])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [zentai.core :as z]
   [es.common :refer (index)]
   [zentai.node :refer (connection)]
   [qbits.spandex :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn get-system-by-host
  "Search for a system using its hostname"
  [host]
  (second
   (or
    (first
     (z/search (index :system)
               {:query {:term {:machine.hostname host}} :size 1})) [])))

(defn missing-systems
  "Filter systems that aren't persisted already"
  [systems]
  (filter
   (fn [{:keys [machine]}] (nil? (get-system-by-host (machine :hostname)))) systems))

(defn exists?
  [id]
  (z/exists? (index :system) :system id))

(defn create
  "create a system returning its id"
  ([system]
   (let [{:keys [status body] :as m} (s/request (connection) {:url [(index :system) :system] :method :post :body system})]
     (assert (#{201 200} status))
     (body :_id)))
  ([system id]
   (= (:status (s/request (connection) {:url [(index :system) :system id] :method :post :body system})) 200)))

(defn put
  "Update a system"
  [id system]
  (z/put (index :system) :system id system))

(defn delete
  "delete a system from ES"
  [id]
  (z/delete (index :system) :system id))

(defn keywordize
  "converting ES values back into keywords"
  [m]
  (when m
    (transform
     [(multi-path [:type] [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword m)))

(defn get
  "Grabs a system by an id, return nil if missing"
  [id]
  (keywordize (z/get (index :system) :system id)))

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
    (= (:status (s/request (connection) {:url [(index :system) :system id] :method :put :body (merge-with merge system part)})) 200)))

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
   (s/request (connection) {:url [(index :system) :_search] :method :get :body {:from from :size size :query query}})))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get (spec :system-id)) ks))

(defn all
  "return all existing systems"
  []
  (mapv (fn [[k v]] [k (keywordize v)]) (z/all (index :system))))
