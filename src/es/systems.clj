(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get partial])
  (:require
   [com.rpl.specter :refer [select transform ALL multi-path]]
   [es.common :refer (index) :as common]
   [es.node :as node :refer (c)]
   [qbits.spandex :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(defn exists?
  [id]
  (= (:status (s/request @c {:url [index :system id] :method :head})) 200))

(defn create
  "create a system returning its id"
  ([system]
   (let [{:keys [status body] :as m} (s/request @c {:url [index :system] :method :post :body system})]
     (assert (#{201 200} status))
     (body :_id)))
  ([system id]
   (= (:status (s/request @c {:url [index :system id] :method :post :body system})) 200)))

(defn put
  "Update a system"
  [id system]
  (= (:status (s/request @c {:url [index :system id] :method :put :body system})) 200))

(defn delete
  "delete a system from ES"
  [id]
  (= (:status (s/request @c {:url [index :system id] :method :delete})) 200))

(defn keywordize
  "converting ES values back into keywords"
  [m]
  (transform
   [(multi-path [:machine :os] [:env] [:kvm :node] [:kvm :volumes ALL :pool])] keyword m))

(defn get
  "Grabs a system by an id, return nil if missing"
  [id]
  (try
    (keywordize
     (get-in (s/request @c {:url [index :system id] :method :get}) [:body :_source]))
    (catch Exception e
      (when-not (= 404 (:status (ex-data e)))
        (throw e)))))

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
    (= (:status (s/request @c {:url [index :system id] :method :put :body (merge-with merge system part)})) 200)))

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
   (s/request @c {:url [index :_search] :method :get :body {:from from :size size :query query}})))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get (spec :system-id)) ks))

(defn all
  "return all existing systems"
  []
  (mapv (fn [[k v]] [k (keywordize v)]) (common/all :system)))
