(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:import (java.util UUID))
  (:use 
    [aws.sdk.ec2 :only 
       (run-instances describe-instances terminate-instances start-instances
        stop-instances instance-filter instance-id-filter)]
    [trammel.core :only (defconstrainedrecord)]
    [celestial.provider :only (str? vec?)]
    [celestial.redis :only (synched-map)]
    [celestial.core :only (Vm)]
    [celestial.common :only (config import-logging curr-time)]
    [mississippi.core :only (required numeric validate)]
    [celestial.model :only (translate vconstruct)]))

(import-logging)

(def instance-valid
  {
   :min-count [(required) (numeric)] 
   :max-count [(required) (numeric)] 
   :instance-type [str? (required)]
   :key-name [str? (required)]
   })

(defn creds [] (get-in config [:hypervisor :aws]))

(def ids (synched-map :aws-keys))

(defn wait-for-status [req-stat instance & {:keys [timeout] :or {timeout 3000}}]
  (let [wait (+ (curr-time) timeout)]
    (loop []
      (if (> wait (curr-time))
        (if (= req-stat (.status instance)) 
          true
          (recur)) 
        false))))

(defmacro ec2 [f & args]
 `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defconstrainedrecord Instance [endpoint conf uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate conf instance-valid))) 
   (not (nil? endpoint)) (instance? UUID uuid)]
  Vm
  (create [this] 
          (debug "creating" uuid)
          (swap! ids assoc uuid (-> (ec2 run-instances conf) :instances first :id)) 
          this)
  (delete [this]
          (debug "deleting" uuid)
          (ec2 terminate-instances (@ids uuid)))
  (start [this]
         (debug "starting" uuid)
         (ec2 start-instances (@ids uuid)))
  (stop [this]
        (debug "stopping" uuid )
        (stop-instances  (@ids uuid)))
  (status [this] 
          (-> (ec2 describe-instances (instance-id-filter (@ids uuid)))
              first :instances first :state :name
              ))) 

(defmethod translate :aws [{:keys [aws machine]}] 
  [(aws :endpoint) (dissoc aws :endpoint) (UUID/randomUUID)])

(defmethod vconstruct :aws [{:keys [aws] :as spec}]
  (apply ->Instance (translate spec)))

(comment 
  (def m (.create (vconstruct celestial.fixtures/redis-ec2-spec))) 
  (.status m) 
  (.delete m)) 

