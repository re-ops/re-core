(ns re-core.presets.aws
  "Presets for AWS https://aws.amazon.com/ec2/instance-types/")

(defn ec2 [instance]
  {:aws {:instance-type instance}})

(defn ebs-volume
  ([size]
   (ebs-volume size "/dev/sdn" "standard"))
  ([size device]
   (ebs-volume size "/dev/sdn"))
  ([size device t]
   {:volumes [{:device device :size size :clear true :volume-type t}]}))

; https://aws.amazon.com/ec2/instance-types/t2/
(def #^{:vcpu 1 :ram 1 :cpu-credit 6} t2-micro (ec2 "t2.micro"))

(def #^{:doc "128Gb EBS volume"} ebs-128G (ebs-volume 128))

(def #^{:doc "256Gb EBS volume"} ebs-256G (ebs-volume 256))

(def #^{:doc "512Gb EBS volume"} ebs-512G (ebs-volume 512))

(def #^{:doc "1TB EBS volume"} ebs-1T (ebs-volume 1024))

(defn refer-aws-presets []
  (require '[re-core.presets.aws :as ap :refer [t2-micro]]))
