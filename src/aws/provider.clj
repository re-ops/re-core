(ns aws.provider
  (:require [aws.sdk.ec2 :as ec2])
  (:import (java.util UUID))
  (:use 
    [flatland.useful.utils :only (defm)]
    [flatland.useful.map :only (dissoc-in*)]
    [minderbinder.time :only (parse-time-unit)]
    [slingshot.slingshot :only  [throw+ try+]]
    [aws.sdk.ec2 :only 
       (run-instances describe-instances terminate-instances start-instances
        stop-instances instance-filter instance-id-filter)]
    [celestial.persistency :only (update-host)]
    [trammel.core :only (defconstrainedrecord)]
    [celestial.provider :only (str? vec?)]
    [celestial.redis :only (synched-map)]
    [celestial.core :only (Vm)]
    [celestial.common :only (config import-logging curr-time)]
    [mississippi.core :only (required numeric validate)]
    [celestial.ssh :only (ssh-up?)]
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
    {:type ::aws:status-failed :message "Timed out on waiting for status" :status req-stat :timeout timeout}))

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

(defn pub-dns [endpoint uuid]
  (instance-desc endpoint uuid :public-dns))

(defn pubdns-update [{:keys [spec endpoint uuid] :as instance}]
  (update-host (get-in spec [:machine :hostname]) 
     {:machine {:ssh-host (pub-dns endpoint uuid)}}))

(defconstrainedrecord Instance [endpoint spec uuid]
  "An Ec2 instance, uuid used for instance-id tracking"
  [(empty? (:errors (validate (spec :aws) instance-valid))) 
   (not (nil? endpoint)) (instance? UUID uuid)]
  Vm
  (create [this] 
          (let [{:keys [aws]} spec]
            (debug "creating" uuid) 
            (swap! (ids) assoc uuid 
                   (-> (ec2 run-instances aws) :instances first :id)) 
            (when (= (image-desc (aws :image-id) :root-device-type) "ebs")
              (wait-for-attach endpoint uuid {:timeout [10 :minute]})) 
            (pubdns-update this))     
          this)
  (delete [this]
          (debug "deleting" uuid)
          (ec2 terminate-instances (instance-id uuid))
          (wait-for-status this "terminated" {:timeout [5 :minute]}) 
          (swap! (ids) dissoc uuid))
  (start [this]
         (debug "starting" (instance-id uuid))
         (ec2 start-instances (instance-id uuid))
         (wait-for-status this "running" {:timeout [5 :minute]})
         (pubdns-update this)
         (assert (ssh-up? (pub-dns uuid endpoint) 22)))
  (stop [this]
        (debug "stopping" uuid )
        (ec2 stop-instances  (instance-id uuid))
        (wait-for-status this "stopped" {:timeout [5 :minute]}))
  (status [this] 
          (try+ 
            (instance-desc endpoint uuid :state :name)
            (catch [:type ::aws:missing-id] e false)))) 

(defmethod translate :aws [{:keys [aws machine] :as spec}] 
  [(aws :endpoint) (dissoc-in* spec [:aws :endpoint]) (UUID/randomUUID)])

(defmethod vconstruct :aws [spec]
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
  ) 

