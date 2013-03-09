(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:import java.util.UUID)
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

(defn wait-for-status [s instance & {:keys [timeout] :or {timeout }}]
 (while (not (= status (.status Instance)))
    
   ) 
  )

(defconstrainedrecord Instance [endpoint conf uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate conf instance-valid))) 
   (not (nil? endpoint)) (instance? UUID uuid)]
  Vm
  (create [this] 
      (debug "creating" conf)
      (swap! ids assoc uuid (-> (ec2/run-instances (creds) conf) :instances first :id)) 
      this)
  (delete [this]
          (debug "deleting" uuid)
          (ec2/terminate-instances (creds) (@ids uuid))
          )
  (start [this]
         (debug "starting" uuid)
         (ec2/start-instances (creds) (@ids uuid)))
  (stop [this]
        (debug "stopping" )
        (ec2/stop-instances (creds)  (@ids uuid)))
  (status [this] 
      (-> (ec2/describe-instances (creds) (ec2/instance-id-filter (@ids uuid)))
          first :instances first :state :name
          ))) 

(defmethod translate :aws [{:keys [aws machine]}] 
  [(aws :endpoint) (dissoc aws :endpoint) (UUID/randomUUID)])

(defmethod vconstruct :aws [{:keys [aws] :as spec}]
    (apply ->Instance (translate spec)))

; (def m (.create (vconstruct celestial.fixtures/redis-ec2-spec)))

; (.status m)

