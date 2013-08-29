(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns aws.provider
  (:require 
    [aws.sdk.ec2 :as ec2]
    [aws.sdk.ebs :as ebs]
    [clojure.string :refer (join)]
    [aws.validations :refer (provider-validation)]
    [celestial.persistency :as p]
    [clojure.core.strint :refer (<<)] 
    [supernal.sshj :refer (execute ssh-up?)] 
    [flatland.useful.utils :refer (defm)] 
    [flatland.useful.map :refer (dissoc-in*)] 
    [slingshot.slingshot :refer  [throw+ try+]] 
    [trammel.core :refer (defconstrainedrecord)] 
    [celestial.provider :refer (wait-for)] 
    [celestial.core :refer (Vm)] 
    [celestial.common :refer (import-logging )] 
    [celestial.model :refer (translate vconstruct hypervisor)]))

(import-logging)

(defn creds [] (hypervisor :aws))

(defn wait-for-status [instance req-stat timeout]
  "Waiting for ec2 machine status timeout is in mili"
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
    {:type ::aws:status-failed :message "Timed out on waiting for status" :status req-stat :timeout timeout}))

(defmacro with-ctx
  "Run ec2 action with context (endpoint and creds)"
  [f & args]
  `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defn image-desc [endpoint ami & ks]
  (-> (ec2/describe-images (assoc (creds) :endpoint endpoint) (ec2/image-id-filter ami))
      first (apply ks)))

(defn instance-desc [endpoint instance-id & ks]
  (-> (with-ctx ec2/describe-instances (ec2/instance-id-filter instance-id))
      first :instances first (get-in ks)))

(defn wait-for-attach [endpoint instance-id timeout]
  (wait-for {:timeout timeout} 
            #(= "attached" (instance-desc endpoint instance-id :block-device-mappings 0 :ebs :status)) 
            {:type ::aws:ebs-attach-failed :message "Failed to wait for ebs root device attach"}))

(defn pub-dns [endpoint instance-id]
  (instance-desc endpoint instance-id :public-dns))

(defn wait-for-ssh [endpoint instance-id user timeout]
    (wait-for {:timeout timeout}
              #(ssh-up? {:host (pub-dns endpoint instance-id) :port 22 :user user})
              {:type ::aws:ssh-failed :message "Timed out while waiting for ssh" :timeout timeout}))

(defn pubdns-to-ip
  "Grabs public ip from dnsname ec2-54-216-121-122.eu-west-1.compute.amazonaws.com"
   [pubdns]
    (join "." (rest (re-find #"ec2\-(\d+)-(\d+)-(\d+)-(\d+).*" pubdns))))

(defn update-pubdns [spec endpoint instance-id]
  "updates public dns in the machine persisted data"
  (when (p/system-exists? (spec :system-id))
    (let [ec2-host (pub-dns endpoint instance-id)]
      (p/partial-system (spec :system-id) {:machine {:ssh-host ec2-host :ip (pubdns-to-ip ec2-host)}}))))

(defn set-hostname [spec endpoint instance-id user]
  "Uses a generic method of setting hostname in Linux"
  (let [hostname (get-in spec [:machine :hostname]) remote {:host (pub-dns endpoint instance-id) :user user}]
    (execute (<< "echo kernel.hostname = ~{hostname} | sudo tee -a /etc/sysctl.conf") remote )
    (execute "sudo sysctl -e -p" remote) 
    (with-ctx ec2/create-tags [(instance-desc endpoint instance-id :id)] {:Name hostname})
    ))

(defn instance-id*
  "grabbing instance id of spec"
   [spec]
  (get-in (p/get-system (spec :system-id)) [:aws :instance-id]))

(defmacro with-instance-id [& body]
 `(if-let [~'instance-id (instance-id* ~'spec)]
    (do ~@body) 
    (throw+ {:type ::aws:missing-id :message "Instance id not found"}))) 

(defn handle-volumes 
   "attached and waits for ebs volumes" 
   [{:keys [image-id volumes]} endpoint instance-id]
  (when (= (image-desc endpoint image-id :root-device-type) "ebs")
    (wait-for-attach endpoint instance-id [10 :minute]))
  (let [zone (instance-desc endpoint instance-id :placement :availability-zone)]
    (doseq [{:keys [device size]} volumes]
      (let [{:keys [volumeId]} (with-ctx ebs/create-volume size zone)]
        (with-ctx ebs/attach-volume volumeId instance-id device)
        (wait-for [10 :minute]
            #(= "attached" (with-ctx ebs/attachment-status volumeId))
            {:type ::aws:ebs-volume-attach-failed :message "Failed to wait for ebs volume device attach"})
        ))))

(defconstrainedrecord Instance [endpoint spec user]
  "An Ec2 instance"
  [(provider-validation spec) (-> endpoint nil? not)]
  Vm
  (create [this] 
    (let [{:keys [aws]} spec instance-id (-> (with-ctx ec2/run-instances (dissoc aws :volumes)) :instances first :id)]
       (p/partial-system (spec :system-id) {:aws {:instance-id instance-id}})
       (debug "created" instance-id)
       (handle-volumes aws endpoint instance-id)    
       (when-let [ip (get-in spec [:machine :ip])] 
         (debug (<<  "Associating existing ip ~{ip} to instance-id"))
       (with-ctx ec2/assoc-pub-ip instance-id ip))
       (update-pubdns spec endpoint instance-id)
       (wait-for-ssh endpoint instance-id user [5 :minute])
       (set-hostname spec endpoint instance-id user)
        this))

  (start [this]
    (with-instance-id
      (debug "starting" instance-id)
      (with-ctx ec2/start-instances instance-id) 
      (wait-for-status this "running" [5 :minute]) 
      (update-pubdns spec endpoint instance-id)))

  (delete [this]
    (with-instance-id
      (debug "deleting" instance-id)
      (with-ctx ec2/terminate-instances instance-id ) 
      (wait-for-status this "terminated" [5 :minute])))

  (stop [this]
    (with-instance-id 
       (debug "stopping" instance-id)
       (with-ctx ec2/stop-instances instance-id) 
       (wait-for-status this "stopped" [5 :minute])))

  (status [this] 
    (try+ 
      (with-instance-id 
            (instance-desc endpoint instance-id :state :name)) 
      (catch [:type ::aws:missing-id] e 
        (warn "No AWS instance id, most chances this instance hasn't been created yet") false))))

(def defaults {:aws {:min-count 1 :max-count 1}})

(defn aws-spec 
  "creates an ec2 spec" 
  [{:keys [aws machine] :as spec}]
  (merge-with merge (dissoc-in* spec [:aws :endpoint]) defaults))

(defmethod translate :aws [{:keys [aws machine] :as spec}] 
  [(aws :endpoint) (aws-spec spec) (or (machine :user) "root")])

(defmethod vconstruct :aws [spec]
  (apply ->Instance (translate spec)))

(comment 
  (use 'celestial.fixtures)
  (def m (vconstruct celestial.fixtures/puppet-ami)) 
  (:endpoint m )
  (.status m)
  (.start m)
  (celestial.model/set-env :dev 
   (ebs/create-volume (assoc (celestial.model/hypervisor :aws) :endpoint "ec2.ap-southeast-2.amazonaws.com") 10 "ap-southeast-2b"))
  (celestial.model/set-env :dev 
   (ebs/delete-volume (assoc (celestial.model/hypervisor :aws) :endpoint "ec2.ap-southeast-2.amazonaws.com") "vol-b0286582"))
  (celestial.model/set-env :dev 
   (ebs/attach-volume (assoc (celestial.model/hypervisor :aws) :endpoint "ec2.ap-southeast-2.amazonaws.com") "vol-a9317c9b" "i-c7b66cfb" "/dev/sdn"))
  (celestial.model/set-env :dev (instance-desc "ec2.ap-southeast-2.amazonaws.com" "i-c7b66cfb" :placement :availability-zone))
  
  ) 


