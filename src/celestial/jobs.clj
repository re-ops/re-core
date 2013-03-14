(ns celestial.jobs
  (:refer-clojure :exclude [identity])
  (:use  
    [celestial.redis :only (create-worker wcar with-lock half-hour minute)]
    [taoensso.timbre :only (debug info error warn trace)]
    [celestial.tasks :only (reload puppetize full-cycle)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def workers (atom {}))

(defn job-exec [f {:keys [identity args] :as spec}]
  "Executes a job function tries to lock identity first (if used)"
  (if identity
    (with-lock identity #(apply f args) {:expiry half-hour :wait-time minute}) 
    (apply f args)))

(def jobs {:machine [reload 2] :provision [puppetize 2] :stage [full-cycle 2]})

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv (fn [v] (create-worker (name queue) (partial job-exec f))) (range total)))

(defn initialize-workers []
  (dosync 
    (doseq [[q [f c]] jobs]
      (swap! workers assoc q (create-wks q f c)))))

(defn clear-all []
  (let [queues (wcar (car/keys ((car/make-keyfn "mqueue") "*")))]
    (when (seq queues) (wcar (apply car/del queues)))))

(defn enqueue [queue payload] 
  (trace "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (debug "shutting down" k w) 
      (carmine-mq/stop w))))

