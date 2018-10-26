(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [zentai.core :as z]
   [es.common :refer (index)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (z/put (index :jobs) :jobs tid job))

(defn delete
  "delete a system from ES"
  [tid]
  (z/delete (index :jobs) :jobs tid))

(defn get
  "Get job bu tid"
  [tid]
  (z/get (index :jobs) :jobs tid))

