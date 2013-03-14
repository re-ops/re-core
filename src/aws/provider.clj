(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:import (java.util UUID))
  (:use 
    [flatland.useful.utils :only (defm)]
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

(defm ids [] (synched-map :aws-keys))

(defn wait-for [{:keys [timeout] :or {timeout [5 :minute]}} pred err]
  "A general wait for pred function"
  (let [wait (+ (curr-time) (parse-time-unit timeout))]
    (loop []
      (if (> wait (curr-time))
        (if (pred) 
          true
          (do (Thread/sleep 2000) (recur))) 
        (throw+ err)))))

(defn wait-for-status [instance req-stat timeout]
  "Waiting for ec2 machine status timeout is in mili"
  (wait-for timeout #(= req-stat (.status instance))
    {:type ::aws:status-failed :message "Failed to wait for status" :status req-stat}))

(defmacro ec2 [f & args]
  `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defn instance-id [uuid]
  (if-let [id (@(ids) uuid)]
    id
    (throw+ 
      {:type ::aws:missing-id :message 
       "Instance id not found, make sure no to address deleted instances" :uuid uuid})
    ))

(defn image-desc [ami & ks]
  (-> (ec2/describe-images (creds) (ec2/image-id-filter ami))
      first (apply ks)))

(defn instance-desc [endpoint uuid & ks]
  (-> (ec2 describe-instances (instance-id-filter (instance-id uuid)))
      first :instances first (get-in ks)))

(defn wait-for-attach [endpoint uuid timeout]
  (wait-for timeout 
    #(= "attached" (instance-desc endpoint uuid :block-device-mappings 0 :ebs :status)) 
    {:type ::aws:ebs-attach-failed :message "Failed to wait for ebs root device attach"}))

(defconstrainedrecord Instance [endpoint conf uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate conf instance-valid))) 
   (not (nil? endpoint)) (instance? UUID uuid)]
  Vm
  (create [this] 
          (debug "creating" uuid)
          (swap! (ids) assoc uuid 
            (-> (ec2 run-instances conf) :instances first :id)) 
          (when (= (image-desc (conf :image-id) :root-device-type) "ebs")
             (wait-for-attach endpoint uuid {:timeout [10 :minute]}))
          this)
  (delete [this]
          (debug "deleting" uuid)
          (ec2 terminate-instances (instance-id uuid))
          (wait-for-status this "terminated" {:timeout [5 :minute]}) 
          (swap! (ids) dissoc uuid))
  (start [this]
         (debug "starting" (instance-id uuid))
         (ec2 start-instances (instance-id uuid))
         (wait-for-status this "running" {:timeout [5 :minute]}))
  (stop [this]
        (debug "stopping" uuid )
        (ec2 stop-instances  (instance-id uuid))
        (wait-for-status this "stopped" {:timeout [2 :minute]}))
  (status [this] 
          (instance-desc endpoint uuid :state :name))) 

(defmethod translate :aws [{:keys [aws machine]}] 
  [(aws :endpoint) (dissoc aws :endpoint) (UUID/randomUUID)])

(defmethod vconstruct :aws [{:keys [aws] :as spec}]
  (apply ->Instance (translate spec)))

(comment 
  (use 'celestial.fixtures)
  (def m (.create (vconstruct celestial.fixtures/redis-ec2-spec))) 
  (.start m)

  (-> (ec2/describe-images (creds) (ec2/image-id-filter "ami-64636a10"))
      first :root-device-type  
      )

  (-> (ec2/describe-images (creds) (ec2/image-id-filter "ami-5a60692e"))
      first :root-device-type  
      )

  (let [endpoint "ec2.eu-west-1.amazonaws.com" uuid (:uuid m)]
    (wait-for-attach endpoint (:uuid m) {:timeout [1 :minute]}) )


  (.status m) 
  (.stop m) 
  (.delete m)

  (wait-for-status m "stopped") 
  (parse-time-unit [1 :minute])
  (wait-for-status m "running" {:timeout [1 :minute]}) 
  ) 


