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
    [celestial.redis :only (create-worker wcar with-lock)]
    [taoensso.timbre :only (debug info error warn trace)]
    [celestial.workflows :only (reload destroy puppetize full-cycle run-action)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as mq]))

(import-logging)

(def workers (atom {}))

(def defaults {:wait-time 5 :expiry 30})

(defn job-exec [f {:keys [identity args tid] :as spec}]
  "Executes a job function tries to lock identity first (if used)"
  (set-tid tid 
    (try 
      (if identity
        (with-lock identity 
         (fn [] (apply f args) :success)
         (map-vals (or (get* :job) defaults) #(* minute %) )) 
        (do (apply f args) :success))
      (catch Throwable e (error e) :error))))

(def jobs 
  (atom 
    {:reload [reload 2] :destroy [destroy 2] :provision [puppetize 2]
     :stage [full-cycle 2] :run-action [run-action 2] }))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec f))) (range total)))

(defn initialize-workers []
  (dosync 
    (doseq [[q [f c]] @jobs]
      (swap! workers assoc q (create-wks q f c)))))

(defn clear-all []
  (doseq [q (map name (keys @jobs))]
    (trace (<< "clearing ~{q} queue"))
    (wcar (mq/clear q))))

(defn enqueue 
  "Placing job in redis queue"
  [queue payload] 
  {:pre [(contains? @jobs (keyword queue))]}
  (trace "submitting" payload "to" queue) 
  (wcar (mq/enqueue queue payload)))

(defn status [queue uuid]
  (wcar (mq/status queue uuid)))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (debug "shutting down" k w) 
      (mq/stop w))))

(defn queue-metadata "Returns given queue's current metadata."
  [qname]
  {:backoff?  (wcar (car/get      (mq/qkey qname "backoff?")))
   :id-circle (wcar (car/lrange   (mq/qkey qname "id-circle") 0 -1))
   :messsages (wcar (car/hgetall* (mq/qkey qname "messages")))
   :recently-done (wcar (car/smembers (mq/qkey qname "recently-done")))
   :locks     (wcar (car/hgetall* (mq/qkey qname "locks")))})

