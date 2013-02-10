(ns celestial.jobs
  (:use  
    [taoensso.timbre :only (debug info error warn)]
    [celestial.tasks :only (reload puppetize)]
    ) 
  (:require  
    [celestial.tasks :only (reload puppetize)]
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as carmine-mq]))

(def pool (car/make-conn-pool)) 
(def spec-server1 (car/make-conn-spec :host "192.168.5.5"))

(def provision-worker
  (carmine-mq/make-dequeue-worker
    pool spec-server1 "provision"
    :handler-fn 
    (fn [provision] 
      (info "starting to provision" provision)
      (puppetize provision)
      (info "done provisioning" provision))))

(def system-worker
  (carmine-mq/make-dequeue-worker
    pool spec-server1 "system"
    :handler-fn 
    (fn [{:keys [system hypervisor]}] 
      (debug "setting up" system "on" hypervisor)
      (reload system hypervisor)
      (debug "done system setup" ))))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn enqueue [queue payload] 
  (debug "submitting" payload "to" queue) 
  (wcar (carmine-mq/enqueue queue payload)))

;(carmine-mq/stop my-worker)
