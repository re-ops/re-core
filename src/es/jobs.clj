(ns es.jobs
  "Jobs ES persistency"
  (:refer-clojure :exclude [get])
  (:require
   [es.node :as node :refer (ES)]
   [es.common :refer (flush- index)]
   [clojurewerkz.elastisch.native.document :as doc]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
  "Add/Update a jobs into ES"
  [{:keys [tid queue status] :as job} ttl & {:keys [flush?]}]
  (doc/put @ES index "jobs" tid (merge job {:queue (name queue) :status (name status)}) {:ttl ttl})
  (when flush? (flush-)))

(defn delete
  "delete a system from ES"
  [tid]
  (doc/delete @ES index "jobs" tid))

(defn get
  "Get job bu tid"
  [id]
  (doc/get @ES index "jobs" id))

(defn query-envs
  "maps envs to query form terms"
  [envs]
  (map (fn [e] {:term {:env (name e)}}) envs))

