(ns re-core.persistency.jobs
  (:require
   [xtdb.api :as xt]
   [re-core.persistency.xtdb :refer [node]]
   [re-core.persistency.common :refer [unflatten flatten-]]))

(defn put
  "Update a job"
  [{:keys [tid] :as job}]
  (xt/await-tx node (xt/submit-tx node [[::xt/put (assoc (flatten- job) :xt/id tid)]])))

(defn delete
  "delete a job"
  [tid]
  (xt/await-tx node (xt/submit-tx node [[::xt/evict tid]])))

(defn get
  "Get a job by tid"
  [tid]
  (unflatten (xt/pull (xt/db node) '[*] tid)))
