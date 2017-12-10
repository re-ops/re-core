(ns es.jobs
  "Jobs ES persistency"
  (:refer-clojure :exclude [get])
  (:require
   [es.node :as node :refer (c)]
   [es.common :refer (index)]
   [clojurewerkz.elastisch.native.document :as doc]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid queue status] :as job} ttl]
  (let [body (merge job {:queue (name queue) :status (name status)})]
    (= (:status (s/request @c {:url [:job tid] :method :put :body body})) 200)))

(defn delete
  "delete a job from ES"
  [tid]
  (doc/delete @ES index "jobs" tid))

(defn get
  "Get job bu tid"
  [id]
  (doc/get @ES index "jobs" id))

