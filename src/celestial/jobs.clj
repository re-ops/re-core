(comment Celestial, Copyright 2012 Ronen Narkis, narkisr.com
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
    [clojure.core.strint :only (<<)]
    [celestial.common :only (get* minute import-logging)]
    [taoensso.carmine.locks :as with-lock])
  (:require  
    [celestial.persistency.systems :as s]
    [es.jobs :as es]
    [flatland.useful.map :refer (map-vals filter-vals)]
    [minderbinder.time :refer (parse-time-unit)]
    [puny.core :refer (entity)]
    [celestial.workflows :as wf]  
    [celestial.security :refer (set-user)]
    [celestial.model :refer (set-env operations)]
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as mq]
    [components.core :refer (Lifecyle)] 
    [celestial.redis :refer (create-worker wcar server-conn clear-locks)]
    ))

(import-logging)

(def workers (atom {}))

(def defaults {:wait-time 5 :expiry 30})

(defn job* 
  "Get job conf value"
   [& ks]
   (get-in (get* :celestial :job) ks))

(defn save-status
   "marks jobs as succesful" 
   [spec status]
  (let [status-exp (* 1000 60 (or (job* :status-expiry) (* 24 60)))]
    (es/put (merge spec {:status status :end (System/currentTimeMillis)}) status-exp :flush? true) 
    (trace "saved status" (merge spec {:status status :end (System/currentTimeMillis)}))
    {:status status}))

(defn job-exec [f  {:keys [message attempt]}]
  "Executes a job function tries to lock identity first (if used)"
  (let [{:keys [identity args tid env user] :as spec} message]
    (set-user user
      (set-env env
        (set-tid tid 
           (let [{:keys [wait-time expiry]} (map-vals (or (job* :lock) defaults) #(* minute %))
                 hostname (when identity (get-in (s/get-system identity) [:machine :hostname]))
                 spec' (merge spec (meta f) {:start (System/currentTimeMillis) :hostname hostname})]
            (try 
              (if identity
                (do (with-lock (server-conn) identity expiry wait-time (apply f args))
                    (save-status spec' :success)) 
                (do (apply f args) 
                    (save-status spec' :success))) 
              (catch Throwable e 
                (error e) 
                (save-status spec' :error)
                ))))))))

(defn jobs []
  {:reload [wf/reload 2] :destroy [wf/destroy 2] :provision [wf/puppetize 2]
   :stage [wf/stage 2] :run-action [wf/run-action 2] :create [wf/create 2]
   :start [wf/start 2] :stop [wf/stop 2] :clear [wf/clear 1] :clone [wf/clone 1]})

(defn apply-config [js]
  {:post [(= (into #{} (keys %)) operations)]} 
  (reduce (fn [m [k [f c]]] (assoc m k [f (or (job* :workers k) c)])) {} js))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec (with-meta f {:queue queue})))) (range total)))

(defn initialize-workers []
  (doseq [[q [f c]] (apply-config (jobs))]
    (swap! workers assoc q (create-wks q f c))))

(defn clear-queues []
  (info "Clearing job queues")
  (apply mq/clear-queues (server-conn) (mapv name (keys (jobs)))))

(defn enqueue 
  "Placing job in redis queue"
  [queue payload] 
  {:pre [(contains? (jobs) (keyword queue))]}
  (trace "submitting" payload "to" queue) 
  (wcar (mq/enqueue queue payload)))

(defn status [queue uuid]
  (wcar (mq/message-status queue uuid)))

(def readable-status
  {:queued :queued :locked :processing :recently-done :done :backoff :backing-off nil :unkown})

(defn- message-desc [type js]
  (mapv 
    (fn [[jid {:keys [identity args tid env] :as message}]] 
      {:type type :status (readable-status (status type jid))
       :env env :id identity :jid jid :tid tid}) (apply hash-map js)))

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
  (reduce (fn [r t] (into r (queue-status (name t)))) [] (keys (jobs))))

(defn by-env 
   "filter jobs status by envs" 
   [envs js]
   (filter (fn [{:keys [env]}] (envs env)) js))

(defn jobs-status [envs]
  (map-vals  
    {:jobs (running-jobs-status)}
    (partial by-env (into #{} envs))))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (trace "Shutting down" k w) 
      (mq/stop w))))

(defrecord Jobs
  []
  Lifecyle
  (setup [this]) 
  (start [this] 
    (info "Starting job workers")
    (when (= (job* :reset-on) :start)
      (clear-queues) 
      (clear-locks))
      (initialize-workers))
  (stop [this]
    (info "Stopping job workers")
    (shutdown-workers)
    (when (= (job* :reset-on) :stop)
      (clear-queues) 
      (clear-locks))))

(defn instance 
   "Creates a jobs instance" 
   []
  (Jobs.))
