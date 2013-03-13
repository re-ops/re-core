(ns celestial.jobs
  (:use  
    [celestial.redis :only (create-worker wcar with-lock half-hour minute)]
    [taoensso.timbre :only (debug info error warn trace)]
    [celestial.tasks :only (reload puppetize full-cycle)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def workers (atom {}))

(defn job-exec [f {:keys [machine] :as spec}]
  {:pre [(machine :hostname)] }
  "Executes a job function tries to lock host first pulls lock info from redis"
  (let [{:keys [hostname]} machine]
    (with-lock hostname #(f spec) {:expiry half-hour :wait-time minute})))

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

