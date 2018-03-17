(ns re-core.presets.aws
  "Presets for AWS https://aws.amazon.com/ec2/instance-types/"
  (:require
   [re-core.common :refer (hostname)]
   [re-core.presets.common :as c]))

(defn security
  "Setting security group"
  [group]
  (fn [instance]
    (assoc-in instance [:aws :security-groups] group)))

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

(defn ec2-machine [os]
  (c/machine "re-ops" "local" os))

(defn ec2
  ([instance]
   (ec2 instance :ubuntu-16.04))
  ([instance os] {:machine (ec2-machine os)
                  :aws {:instance-type instance
                        :key-name hostname
                        :endpoint "ec2.ap-southeast-2.amazonaws.com"
                        :security-groups ["default"]
                        :ebs-optimized false}}))

; https://aws.amazon.com/ec2/instance-types/t2/
(def #^{:vcpu 1 :ram 0.5 :cpu-credit 3} t2-nano (ec2 "t2.nano"))

(def #^{:vcpu 1 :ram 1 :cpu-credit 6} t2-micro (ec2 "t2.micro"))

(def #^{:vcpu 1 :ram 2 :cpu-credit 12} t2-small (ec2 "t2.small"))

; https://aws.amazon.com/ec2/instance-types/c5/

(def #^{:vcpu 2 :ram 4 :ebs-optimized true} c5-large (ec2 "c5.large"))

(def #^{:doc "128Gb EBS volume"} ebs-128G (ebs-volume 128))

(def #^{:doc "256Gb EBS volume"} ebs-256G (ebs-volume 256))

(def #^{:doc "512Gb EBS volume"} ebs-512G (ebs-volume 512))

(def #^{:doc "1TB EBS volume"} ebs-1T (ebs-volume 1024))

(defn refer-aws-presets []
  (require '[re-core.presets.aws :as ap :refer [t2-nano t2-micro t2-small c5-large security endpoint key-name
                                                eph-volume]]))