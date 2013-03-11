(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:import (java.util UUID))
  (:use 
    [minderbinder.time :only (parse-time-unit)]
    [slingshot.slingshot :only  [throw+ try+]]
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

(defn wait-for-status [instance req-stat  {:keys [timeout] :or {timeout [5 :minute]}}]
  "Waiting for ec2 machine status timeout is in mili"
  (let [wait (+ (curr-time) (parse-time-unit timeout))]
    (loop []
      (if (> wait (curr-time))
        (if (= req-stat (.status instance)) 
          true
          (do (Thread/sleep 2000) (recur))) 
        (throw+ {:type ::aws:status-failed :message "Failed to wait for status" :status req-stat})))))

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
          (ec2 terminate-instances (@ids uuid))
          (wait-for-status this "terminated" {:timeout [5 :minute]}) 
          (swap! ids dissoc uuid))
  (start [this]
         (debug "starting" uuid)
         (ec2 start-instances (@ids uuid))
         (wait-for-status this "running" {:timeout [5 :minute]}))
  (stop [this]
        (debug "stopping" uuid )
        (ec2 stop-instances  (@ids uuid))
        (wait-for-status this "stopped" {:timeout [2 :minute]}))
  (status [this] 
          (-> (ec2 describe-instances (instance-id-filter (@ids uuid)))
              first :instances first :state :name
              ))) 

(defmethod translate :aws [{:keys [aws machine]}] 
  [(aws :endpoint) (dissoc aws :endpoint) (UUID/randomUUID)])

(defmethod vconstruct :aws [{:keys [aws] :as spec}]
  (apply ->Instance (translate spec)))

(comment 
  (use 'celestial.fixtures)
  (def m (.create (vconstruct celestial.fixtures/redis-ec2-spec))) 
  (.start m)
  (.status m) 
  (.stop m) 
  (wait-for-status m "stopped") 
  (.delete m)
  (parse-time-unit [1 :minute])
  (wait-for-status m "running" {:timeout [1 :minute]}) 
  ) 


