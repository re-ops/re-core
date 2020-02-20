(ns re-core.presets.systems
  "Common preset functions"
  (:require
   [expound.alpha :as expound]
   [re-core.specs :as core]
   [re-core.presets.instance-types :refer (c1-medium c4-large)]
   [clojure.spec.alpha :as s]
   [re-core.model :refer (figure-virt)]
   [re-core.presets.digitial :as d]
   [re-core.presets.aws :as amz]
   [re-share.core :refer (gen-uuid)]))

(defn name-gen
  "Generating a unique hostname from host/type + uuid"
  [instance]
  (update-in instance [:machine :hostname]
             (fn [hostname] (str hostname "-" (.substring (gen-uuid) 0 10)))))

(defn into-spec
  "Get the args applied on our base structure by type"
  [m args]
  (if (empty? args)
    m
    (let [a (first args) {:keys [fns]} m]
      (cond
        (string? a) (into-spec (assoc m :description a) (rest args))
        (number? a) (into-spec (assoc m :total a) (rest args))
        (keyword? a) (into-spec (assoc m :type a) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn with-description [d]
  (fn [instance] (assoc instance :description d)))

(defn with-type [t]
  (fn [instance] (assoc instance :type t)))

(defn with-host [t]
  (fn [{:keys [type] :as instance}]
    (assoc-in instance [:machine :hostname] (name type))))

(defn materialize-preset
  "Convert a preset into a system using provided args (functions, keyswords etc..)"
  [base args]
  (let [{:keys [fns total type description hostname]} (into-spec {} args)
        transforms [(with-type type) (with-host type) name-gen (with-description description)]
        all (apply conj transforms fns)]
    (map
     (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))))

(defn validate
  "Group systems by ::core/system specification validation result
   Failing results are converted into expound format output string"
  [sp]
  (update-in
   (group-by (partial s/valid? ::core/system) sp)
   [false] (partial map (partial expound/expound ::core/system))))

(defn os
  "Set the os version"
  [k]
  (fn [instance]
    (assoc-in instance [:machine :os] k)))

(def ubuntu-19_10 (os :ubuntu-19.10))

(defn machine
  "A base machine template with only user and domain populated"
  [user domain]
  (fn [instance]
    (update instance :machine
            (fn [m] (merge m {:user user :domain domain})))))

(def ^{:doc "A default base machine with user and domain"} default-machine (machine "re-ops" "local"))

(defn defaults
  "Applying default setting for our preset check the matching aws/defaults and digital-ocean/defaults for details per hypervisor information"
  [instance]
  (let [base (-> instance (ubuntu-19_10) (default-machine))]
    (case (figure-virt base)
      :digital-ocean (d/defaults base)
      :aws (amz/defaults base)
      base)))

(defn node
  "Selecting a node to create the instance under:
  
    (create <hypervisor> (node :remote-node) ....)
  "
  [n]
  (fn [instance]
    (assoc-in instance [(figure-virt instance) :node] n)))

(def ^{:doc "Selecting localhost hypervisor node"} local (node :localhost))

(def ^{:doc "Creating an lxc container (create lxc ...)"} lxc {:lxc {} :machine {}})

(def ^{:doc "Creating a kvm VM: (create kvm ...)"} kvm {:kvm {} :machine {}})

(def ^{:doc "Creating a ec2 VM: (create ec2 ...)"} ec2 {:aws {} :machine {}})

(def ^{:doc "Adding a physical machine: (add physical  ...)"} physical {:physical {} :machine {}})

(def ^{:doc "Creating a ec2 VM: (create ec2 ...)"} droplet {:digital-ocean {} :machine {}})

(defn dispoable-instance
  "Creating a default Ubuntu desktop c4-large instance"
  []
  (validate (materialize-preset kvm [default-machine local (os :ubuntu-desktop-19.10) c4-large :disposable "A temporary sandbox"])))

(defn refer-system-presets []
  (require '[re-core.presets.systems :as spc :refer [node lxc kvm droplet ec2 os ubuntu-19_10 defaults local default-machine]]))
