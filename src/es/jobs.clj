(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [qbits.spandex :as s]
   [es.node :as node :refer (c)]
   [es.common :refer (index)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid queue status] :as job} ttl]
  (let [body (merge job {:queue (name queue) :status (name status)})]
    (= (:status (s/request @c {:url [index :jobs tid] :method :put :body body})) 200)))

(defn delete
  "delete a job from Elasticsearch"
  [tid]
  (= (:status (s/request @c {:url [index :jobs tid] :method :delete})) 200))

(defn get
  "Get job bu tid"
  [id]
  (:_source (s/request @c {:url [index :jobs id] :method :get})))

