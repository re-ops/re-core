(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns celestial.jobs
  (:refer-clojure :exclude [identity])
  (:use   
    [gelfino.timbre :only (set-tid)]
    [celestial.common :only (get*)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (minute import-logging)]
    [celestial.redis :only (create-worker wcar server-conn)]
    [taoensso.timbre :only (debug info error warn trace)]
    [taoensso.carmine.locks :as with-lock])
  (:require  
    [flatland.useful.map :refer (map-vals filter-vals)]
    [minderbinder.time :refer (parse-time-unit)]
    [puny.core :refer (entity)]
    [celestial.workflows :as wf]  
    [celestial.security :refer (set-user)]
    [celestial.model :refer (set-env)]
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as mq]))

(import-logging)

(def workers (atom {}))

(def defaults {:wait-time 5 :expiry 30})

(entity job-status :indices [status])

(defn validate-job-status [{:keys [status] :as js}] (assert status))

(defn save-status
   "marks jobs as succesful" 
   [spec status]
  (let [id (add-job-status (merge spec {:status status :end (System/currentTimeMillis)})) 
        status-exp (* 60 (or (get* :celestial :job :status-expiry) 5))]
    (wcar (car/expire (job-status-id id) status-exp)) {:status status}))

(defn job-exec [f  {:keys [message attempt]}]
  "Executes a job function tries to lock identity first (if used)"
  (let [{:keys [identity args tid env user] :as spec} message 
        spec' (merge spec (meta f) {:start (System/currentTimeMillis)})]
    (set-user user
      (set-env env
        (set-tid tid 
           (let [{:keys [wait-time expiry]} (map-vals (or (get* :celestial :job :lock) defaults) #(* minute %) )]
            (try 
              (if identity
                (do (with-lock (server-conn) identity expiry wait-time (apply f args)) (save-status spec' :success)) 
                (do (apply f args) (save-status spec' :success))) 
              (catch Throwable e (error e) (save-status spec' :error)))))))))

(def jobs 
  (atom 
    {:reload [wf/reload 2] :destroy [wf/destroy 2] :provision [wf/puppetize 2]
     :stage [wf/stage 2] :run-action [wf/run-action 2] :create [wf/create 2]
     :start [wf/start 2]  :stop [wf/stop 2] :clear [wf/clear 1]
     }))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec (with-meta f {:queue queue})))) (range total)))

(defn initialize-workers []
  (dosync 
    (doseq [[q [f c]] @jobs]
      (swap! workers assoc q (create-wks q f c)))))

(defn clear-all []
  (apply mq/clear-queues (server-conn) (mapv name (keys @jobs))))

(defn enqueue 
  "Placing job in redis queue"
  [queue payload] 
  {:pre [(contains? @jobs (keyword queue))]}
  (trace "submitting" payload "to" queue) 
  (wcar (mq/enqueue queue payload)))

(defn status [queue uuid]
  (mq/message-status (server-conn) queue uuid))

(def readable-status
  {:queued :queued :locked :processing :recently-done :done :backoff :backing-off nil :unkown})

(defn- message-desc [type js]
  (mapv (fn [[jid {:keys [identity args tid] :as message}]] 
          {:type type :status (readable-status (status type jid)) :id identity :jid jid :tid tid}) (apply hash-map js)))

(defn queue-status 
  "The entire queue message statuses" 
  [job]
  (let [ks [:messages :locks :backoffs]]
    (reduce 
      (fn [r message] (into r (message-desc job message))) []
      (apply merge (vals (select-keys (mq/queue-status (server-conn) job) ks))))))

(defn running-jobs-status
  "Get all jobs status" 
  []
  (reduce (fn [r t] (into r (queue-status (name t)))) [] (keys @jobs)))

(defn clear-and-get 
  "clears on a missing job-status" 
  [id]
  (let [s (get-job-status id)]
    (if-not (empty? s) s
      (do (wcar (clear-job-status-indices id :status)) nil)  ))) 

(defn done-jobs-status 
  "Grab done jobs statuses (changes every time due to expiry of keys)" 
  []
  (let [succesful (get-job-status-index :status :success)
        erroneous (get-job-status-index :status :error)]
    {:erroneous (filter clojure.core/identity (map clear-and-get erroneous)) 
     :succesful (filter clojure.core/identity (map clear-and-get succesful))}))

(defn by-env 
   "filter jobs status by envs" 
   [envs js]
   (filter (fn [{:keys [env]}] (envs env)) js))

(defn jobs-status [envs]
  (map-vals  
    (merge {:jobs (running-jobs-status)} (done-jobs-status)) 
    (partial by-env (into #{} envs))))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (trace "shutting down" k w) 
      (mq/stop w))))

