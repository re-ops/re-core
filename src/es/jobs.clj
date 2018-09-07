(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [re-share.es.common :as common]
   [es.common :refer (index)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (common/put (index :jobs) :jobs tid job))

(defn delete
  "delete a system from ES"
  [tid]
  (common/delete (index :jobs) :jobs tid))

(defn get
  "Get job bu tid"
  [tid]
  (common/get (index :jobs) :jobs tid))

