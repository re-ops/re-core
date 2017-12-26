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
  [{:keys [tid] :as job}]
  (= (:status (s/request @c {:url [index :jobs tid] :method :put :body job})) 200))

(defn delete
  "delete a job from Elasticsearch"
  [tid]
  (= (:status (s/request @c {:url [index :jobs tid] :method :delete})) 200))

(defn get
  "Get job bu tid"
  [tid]
  (try
    (let [result (s/request @c {:url [index :jobs tid] :method :get})]
      (get-in result [:body :_source]))
    (catch Exception e
      (when-not (= 404 (:status (ex-data e)))
        (throw e)))))

