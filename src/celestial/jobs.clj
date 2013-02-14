(ns celestial.jobs
  (:use  
    [celestial.redis :only (wcar create-worker)]
    [taoensso.timbre :only (debug info error warn)]
    [celestial.tasks :only (reload puppetize full-cycle)]) 
  (:require  
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))


(def workers (atom {}))

(defn initialize-workers []
  (dosync 
    (doseq [[q f] {:machine reload :provision puppetize :stage full-cycle}]
      (swap! workers assoc q (create-worker (name q) f)))))

(defn clear-all []
  (let [queues (wcar (car/keys ((car/make-keyfn "mqueue") "*")))]
    (when (seq queues) (wcar (apply car/del queues)))))

(defn enqueue [queue payload] 
  (debug "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

;(carmine-mq/stop my-worker)
