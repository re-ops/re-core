(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:use 
    [celestial.model :only (translate vconstruct)]
    [celestial.common :only (config import-logging)])
    [celestial.model :only (translate vconstruct)]
  )

(import-logging)

(defn creds [] (get-in config [:hypervisor :aws]))

(defconstrainedrecord Instance [endpoint instance-conf extended]
  "An Ec2 instance"
  [(empty? (:errors (validate instance ct-valid))) (not (nil? node))]
  Vm
  (create [this] 
          (debug "creating" )
          (ec2/run-instances (creds) instance))
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
  (let [{:keys [endpoint]} ec2]
    (let [instance-conf (translate spec)] 
      (->Instance endpoint ct ex))))
