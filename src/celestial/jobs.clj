(ns celestial.jobs
  (:use  
    [celestial.redis :only (with-lock)]
    [celestial.redis :only (wcar create-worker)]
    [taoensso.timbre :only (debug info error warn)]
    [celestial.tasks :only (reload puppetize full-cycle)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def workers (atom {}))

(defn job-exec [f spec]
  "Executes a job function tries to lock host first pulls lock info from redis"
  (let [{:keys [host]} spec]
    (with-lock host #(f spec))))

(defn initialize-workers []
  (dosync 
    (doseq [[q f] {:machine reload :provision puppetize :stage full-cycle}]
      (swap! workers assoc q (create-worker (name q) (partial job-exec f))))))

(defn clear-all []
  (let [queues (wcar (car/keys ((car/make-keyfn "mqueue") "*")))]
    (when (seq queues) (wcar (apply car/del queues)))))

(defn enqueue [queue payload] 
  (debug "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

(defn shutdown-workers []
  (doseq [[k w] @workers]
    (debug "shutting down" w)
    (carmine-mq/stop w)))
