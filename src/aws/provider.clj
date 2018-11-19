(ns aws.provider
  (:require
   [amazonica.aws.ec2 :as ec2]
   [aws.common :refer (with-ctx instance-desc creds image-id)]
   [aws.networking :refer
    (update-ip set-hostname instance-ip assoc-pub-ip describe-eip)]
   [aws.volumes :refer (delete-volumes handle-volumes)]
   [aws.validations :refer (provider-validation)]
   [clojure.core.strint :refer (<<)]
   [re-mote.ssh.transport :refer (ssh-up?)]
   [flatland.useful.utils :refer (defm)]
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.provider :refer (wait-for-ssh map-key)]
   [re-share.core :refer (wait-for)]
   [re-core.core :refer (Vm)]
   [taoensso.timbre :as timbre]
   [es.systems :as s]
   [re-core.model :refer (translate vconstruct hypervisor*)])
  (:import clojure.lang.ExceptionInfo))

(timbre/refer-timbre)

(defn wait-for-status
  "Waiting for ec2 machine status timeout is in mili"
  [instance req-stat timeout]
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
            "Timed out on waiting for status"))

(defn instance-id*
  "grabbing instance id of spec"
  [spec]
  (get-in (s/get (spec :system-id)) [:aws :instance-id]))

(defmacro with-instance-id [& body]
  `(if-let [~'instance-id (instance-id* ~'spec)]
     (do ~@body)
     (throw (ex-info "Instance id not found" {:type ::aws:missing-id}))))

(defn creation-keys [aws]
  (clojure.set/subset? (into #{} (keys aws))
                       #{:volumes :min-count :max-count :instance-type :ebs-optimized
                         :key-name :placement :security-groups :network-interfaces
                         :subnet-id :block-device-mappings}))

(defn create-instance
  "creates instance from aws"
  [{:keys [aws machine] :as spec} endpoint]
  {:pre [(creation-keys aws)]}
  (let [inst (merge (dissoc aws :volumes) {:image-id (image-id machine)})
        {:keys [reservation]} (with-ctx ec2/run-instances inst)]
    (get-in reservation [:instances 0 :instance-id])))

(def ssh-timeout 5)

(defrecord Instance [endpoint spec user]
  Vm
  (create [this]
    (let [instance-id (create-instance spec endpoint)]
      (s/partial (spec :system-id) {:aws {:instance-id instance-id}})
      (debug "created" instance-id)
      (handle-volumes spec endpoint instance-id)
      (when (get-in spec [:machine :ip])
        (debug (<<  "Associating existing ip to ~{instance-id}"))
        (assoc-pub-ip endpoint instance-id spec))
      (update-ip spec endpoint instance-id)
      (wait-for-ssh (instance-ip spec endpoint instance-id) user [ssh-timeout :minute])
      (info "ssh is up for" instance-id)
      (set-hostname spec endpoint instance-id user)
      this))

  (start [this]
    (with-instance-id
      (debug "starting" instance-id)
      (with-ctx ec2/start-instances {:instance-ids [instance-id]})
      (wait-for-status this "running" [5 :minute])
      (when (get-in spec [:machine :ip])
        (debug (<<  "Associating existing ip to ~{instance-id}"))
        (assoc-pub-ip endpoint instance-id spec))
      (update-ip spec endpoint instance-id)
      (wait-for-ssh (instance-ip spec endpoint instance-id) user [ssh-timeout :minute])))

  (delete [this]
    (with-instance-id
      (debug "deleting" instance-id)
      (delete-volumes endpoint instance-id (spec :system-id))
      (with-ctx ec2/terminate-instances {:instance-ids [instance-id]})
      (wait-for-status this "terminated" [5 :minute])
      ; for reload support
      (s/put (spec :system-id)
             (dissoc-in* (s/get (spec :system-id)) [:aws :instance-id]))))

  (stop [this]
    (with-instance-id
      (debug "stopping" instance-id)
      (when-not (first (:addresses (describe-eip endpoint instance-id)))
        (debug "clearing dynamic public ip from system")
        (s/put (spec :system-id)
               (dissoc-in* (s/get (spec :system-id)) [:machine :ip])))
      (with-ctx ec2/stop-instances {:instance-ids [instance-id]})
      (wait-for-status this "stopped" [5 :minute])))

  (status [this]
    (try
      (with-instance-id
        (instance-desc endpoint instance-id :state :name))
      (catch ExceptionInfo e
        (when (= (-> e ex-data :type) ::aws:missing-id)
          (debug "No AWS instance id, most chances this instance hasn't been created yet")) false)))

  (ip [this]
    (with-instance-id
      (instance-ip spec endpoint instance-id))))

(def defaults {:aws {:min-count 1 :max-count 1}})

(defn find-groups
  "Finding security groups under vpc"
  [endpoint names vpc-id]
  {:pre [endpoint]}
  (map :group-id
       (filter (fn [{:keys [group-id group-name]}] ((into #{} names) group-name))
               (:security-groups
                (with-ctx ec2/describe-security-groups {:filters [{:name "vpc-id" :values [vpc-id]}]})))))

(defn assign-vpc
  "Attach vpc info"
  [endpoint {:keys [aws] :as spec}]
  (let [{:keys [subnet-id assign-public vpc-id]} (or (:vpc aws) (hypervisor* :aws :default-vpc))
        groups (find-groups endpoint (:security-groups aws) vpc-id)
        public [{:groups groups :device-index 0 :associate-public-ip-address assign-public :subnet-id subnet-id}]]
    (-> spec
        (assoc-in [:aws :network-interfaces] public)
        (dissoc-in* [:aws :vpc])
        (dissoc-in* [:aws :security-groups]))))

(defn aws-spec
  "Creates an ec2 spec"
  [{:keys [aws machine] :as spec}]
  (let [spec' (merge-with merge (dissoc-in* spec [:aws :endpoint]) defaults)]
    (assign-vpc (get-in spec [:aws :endpoint])
                (cond-> spec'
                  (get-in spec' [:aws :availability-zone])
                  (map-key [:aws :availability-zone] [:aws :placement :availability-zone])
                  (get-in spec' [:aws :block-devices])
                  (map-key [:aws :block-devices] [:aws :block-device-mappings])))))

(defmethod translate :aws [{:keys [aws machine] :as spec}]
  [(aws :endpoint) (aws-spec spec) (or (machine :user) "root")])

(defn validate [[endpoint spec user :as args]]
  (provider-validation spec)
  (assert (not (nil? endpoint)))
  args)

(defmethod vconstruct :aws [spec]
  (apply ->Instance (validate (translate spec))))

