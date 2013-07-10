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
    [flatland.useful.map :on map-vals]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (minute import-logging)]
    [celestial.redis :only (create-worker wcar server-conn)]
    [taoensso.timbre :only (debug info error warn trace)]
    [taoensso.carmine.locks :as with-lock]
    [celestial.workflows :only (reload destroy puppetize stage run-action)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as mq]))

(import-logging)

(def workers (atom {}))

(def defaults {:wait-time 5 :expiry 30})

(defn job-exec [f  {:keys [message attempt]}]
  "Executes a job function tries to lock identity first (if used)"
  (let [{:keys [identity args tid] :as spec} message]
   (set-tid tid 
    (let [{:keys [wait-time expiry]} (map-vals (or (get* :celestial :job) defaults) #(* minute %) )]
      (try 
       (if identity
         (do (with-lock (server-conn) identity wait-time expiry (apply f args)) {:status :success}) 
         (do (apply f args) {:status :success})) 
       (catch Throwable e (error e) {:status  :error}))))))

(def jobs 
  (atom 
    {:reload [reload 2] :destroy [destroy 2] :provision [puppetize 2]
     :stage [stage 2] :run-action [run-action 2] }))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec f))) (range total)))

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

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (debug "shutting down" k w) 
      (mq/stop w))))

