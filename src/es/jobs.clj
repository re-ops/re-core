(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [rubber.core :as z]
   [es.common :refer (index)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (z/put (index :jobs) tid job))

(defn delete
  "delete a system from ES"
  [tid]
  (z/delete (index :jobs) tid))

(defn get
  "Get job bu tid"
  [tid]
  (z/get (index :jobs) tid))

