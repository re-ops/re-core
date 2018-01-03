(ns es.jobs
  "Jobs Elasticsearch persistency"
  (:refer-clojure :exclude [get])
  (:require
   [qbits.spandex :as s]
   [es.node :as node :refer (c)]
   [es.common :refer (index) :as common]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (common/put index :jobs tid job))

(defn delete
  "delete a system from ES"
  [tid]
  (common/delete index :jobs tid))

(defn get
  "Grabs a job by a tid, return nil if missing"
  [id]
  (common/get index :system id))

(defn get
  "Get job bu tid"
  [tid]
  (common/get index :jobs tid))

