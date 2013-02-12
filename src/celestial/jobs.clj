(ns celestial.jobs
  (:use  
    [taoensso.timbre :only (debug info error warn)]
    [celestial.tasks :only (reload puppetize)]) 
  (:require  
    [celestial.tasks :only (reload puppetize)]
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def pool (car/make-conn-pool)) 

; TODO move to config file
(def spec-server1 (car/make-conn-spec :host "localhost"))

(def workers (atom {}))

(defn initialize-workers []
  (dosync 
    (doseq [[q f] {:system reload :provision puppetize}]
      (swap! workers assoc q
         (carmine-mq/make-dequeue-worker pool spec-server1 (name q) :handler-fn f)))))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn clear-all []
  (let [queues (wcar (car/keys ((car/make-keyfn "mqueue") "*")))]
    (when (seq queues) (wcar (apply car/del queues)))))

(defn enqueue [queue payload] 
  (debug "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

;(carmine-mq/stop my-worker)
