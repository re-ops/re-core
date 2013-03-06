(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:use 
    [trammel.core :only  (defconstrainedrecord)]
    [celestial.provider :only (str? vec?)]
    [celestial.model :only (translate vconstruct)]
    [celestial.core :only (Vm)]
    [celestial.common :only (config import-logging)]
    [mississippi.core :only (required numeric validate)]
    [celestial.model :only (translate vconstruct)]) 
  )

(import-logging)

(def instance-valid
  {
   :min-count [(required) (numeric)] 
   :max-count [(required) (numeric)] 
   :instance-type [str? (required)]
   :keyname [str? (required)]
   :endpoint [str? (required)]
   })

(defn creds [] (get-in config [:hypervisor :aws]))

(def ids (atom {}))

(defconstrainedrecord Instance [endpoint conf uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate conf instance-valid))) (not (nil? endpoint))]
  Vm
  (create [this] 
          (debug "creating" )
          (ec2/run-instances (creds) conf))
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

(defmethod vconstruct :aws [{:keys [ec2] :as spec}]
  (let [{:keys [endpoint]} ec2 [conf uuid] (translate spec)]
    (->Instance endpoint conf uuid)))
