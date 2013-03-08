(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:use 
    [trammel.core :only  (defconstrainedrecord)]
    [celestial.provider :only (str? vec?)]
    [celestial.redis :only (synched-map)]
    [celestial.core :only (Vm)]
    [celestial.common :only (config import-logging)]
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

(defconstrainedrecord Instance [endpoint conf uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate conf instance-valid))) (not (nil? endpoint))]
  Vm
  (create [this] 
          (debug "creating" conf)
          (debug (ec2/run-instances (creds) conf)))
  (delete [this]
          (debug "deleting"))
  (start [this]
         (debug "starting" )
         (ec2/start-instances (creds)  "i-8843dec2"))
  (stop [this]
        (debug "stopping" )
        (ec2/stop-instances (creds)  "i-8843dec2"))
  (status [this] 
          (ec2/describe-instances (creds)))) 

(defmethod translate :aws [{:keys [aws machine]}] 
   [(dissoc aws :endpoint) (java.util.UUID/randomUUID) (aws :endpoint) ] )

(defmethod vconstruct :aws [{:keys [aws] :as spec}]
  (let [[conf endpoint uuid] (translate spec)]
    (->Instance endpoint conf uuid)))

; (use 'celestial.fixtures)
; (.create (vconstruct redis-ec2-spec))

