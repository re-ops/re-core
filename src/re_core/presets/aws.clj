(ns re-core.presets.aws
  "Presets for AWS https://aws.amazon.com/ec2/instance-types/"
  (:require
   [re-core.common :refer (hostname)]))

(defn security
  "Setting security group"
  [group]
  (fn [instance]
    (assoc-in instance [:aws :security-groups] [group])))

(defn endpoint
  "Setting endpoint"
  [endpoint]
  (fn [instance]
    (assoc-in instance [:aws :endpoint] endpoint)))

(defn key-name
  "Setting keyname"
  [key-name]
  (fn [instance]
    (assoc-in instance [:aws :key-name] key-name)))

(defn eph-volume
  ([]
   (eph-volume "/dev/sdb" "ephemeral0"))
  ([device name]
   (fn [instance]
     (update-in instance [:aws :volumes]
                (fn [vs]
                  (conj vs {:device-name device :virtual-name name}))))))

(defn vpc
  "Attach vpc information"
  ([id subnet]
   (vpc id subnet false))
  ([id subnet public]
   (fn [instance]
     (assoc-in instance [:aws :vpc] {:assign-public public :subnet-id subnet :vpc-id id}))))

(defn ebs-volume
  ([size]
   (ebs-volume size "/dev/sdn" "standard"))
  ([size device]
   (ebs-volume size "/dev/sdn"))
  ([size device t]
   (fn [instance]
     (update-in instance [:aws :volumes]
                (fn [vs]
                  (conj vs {:device device :size size :clear true :volume-type t}))))))

; regions
(def ap-southeast-2 (endpoint "ec2.ap-southeast-2.amazonaws.com"))

(def eu-west-1 (endpoint "ec2.eu-west-1.amazonaws.com"))

(def us-east-1 (endpoint "ec2.us-east-1.amazonaws.com"))

(defn defaults [instance]
  (update instance :aws
          (fn [m]
            (merge m
                   {:key-name hostname
                    :endpoint "ec2.ap-southeast-2.amazonaws.com"
                    :security-groups ["default"]
                    :ebs-optimized false}))))

(def ^{:doc "128Gb EBS volume"} ebs-128G (ebs-volume 128))

(def ^{:doc "256Gb EBS volume"} ebs-256G (ebs-volume 256))

(def ^{:doc "512Gb EBS volume"} ebs-512G (ebs-volume 512))

(def ^{:doc "1TB EBS volume"} ebs-1T (ebs-volume 1024))

(defn refer-aws-presets []
  (require '[re-core.presets.aws :as asp :refer [security endpoint key-name eph-volume vpc ap-southeast-2 eu-west-1 us-east-1]]))
