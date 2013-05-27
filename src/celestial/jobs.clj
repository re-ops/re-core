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
    [celestial.redis :only (create-worker wcar with-lock half-hour minute)]
    [taoensso.timbre :only (debug info error warn trace)]
    [celestial.workflows :only (reload destroy puppetize full-cycle run-task)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def workers (atom {}))

(defn job-exec [f {:keys [identity args] :as spec}]
  "Executes a job function tries to lock identity first (if used)"
  (if identity
    (with-lock identity #(apply f args) {:expiry half-hour :wait-time minute}) 
    (apply f args)))

(def jobs 
  (atom 
    {:reload [reload 2] :destroy [destroy 2] :provision [puppetize 2]
     :stage [full-cycle 2] :remote [run-task 2] }))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec f))) (range total)))

(defn initialize-workers []
  (dosync 
    (doseq [[q [f c]] @jobs]
      (swap! workers assoc q (create-wks q f c)))))

(defn clear-all []
  (let [queues (wcar (car/keys ((car/make-keyfn "mqueue") "*")))]
    (when (seq queues) (wcar (apply car/del queues)))))

(defn enqueue [queue payload] 
  (trace "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

(defn status [queue uuid]
   (wcar (carmine-mq/status queue uuid)))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (debug "shutting down" k w) 
      (carmine-mq/stop w))))

