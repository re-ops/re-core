(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [es.common :refer (index)]
   [re-share.es.common :as common]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (common/put (index) :jobs tid job))

(defn delete
  "delete a system from ES"
  [tid]
  (common/delete (index) :jobs tid))

(defn get
  "Get job bu tid"
  [tid]
  (common/get (index) :jobs tid))

