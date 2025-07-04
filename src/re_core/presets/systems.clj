(ns re-core.presets.systems
  "Common preset functions"
  (:require
   [re-share.config.core :as c]
   [expound.alpha :as expound]
   [re-core.specs :as core]
   [re-core.presets.instance-types :refer (c1-medium c4-large)]
   [clojure.spec.alpha :as s]
   [re-core.model :refer (figure-virt)]
   [re-core.presets.digital :as d]
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

(defn with-description
  "Setting system desciption (by default the string arg)"
  [d]
  (fn [instance] (assoc instance :description d)))

(defn with-type
  "The system type (default uses the keyword type)"
  [t]
  (fn [instance] (assoc instance :type t)))

(defn with-host
  "Set system host (default is type)"
  [hostname]
  (fn [instance]
    (assoc-in instance [:machine :hostname] hostname)))

(defn materialize-preset
  "Convert a preset into a system using provided args (functions, keyswords etc..)"
  [base args]
  (let [{:keys [fns total type description]} (into-spec {} args)
        transforms [(with-type type) (with-host (name type)) name-gen (with-description description)]
        ; user provided transform will override these by ordering (will be applied last)
        all (apply conj transforms fns)]
    (map
     (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))))

(defn validate
  "Group systems by ::core/system specification validation result
   Failing results are converted into expound format output string"
  [sp]
  (let [by-valid (group-by (partial s/valid? ::core/system) sp)
        output-spec {:print-specs? false :show-valid-values? false}]
    (update-in by-valid [false]
               (fn [failures]
                 (map (fn [m] {:message (expound/expound-str ::core/system m) :args [m]}) failures)))))

(defn os
  "Set the os version"
  [k]
  (fn [instance]
    (assoc-in instance [:machine :os] k)))

(def ubuntu-22_04 (os :ubuntu-22.04))

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
  (let [base (-> instance (ubuntu-22_04) default-machine)]
    (case (figure-virt base)
      :digital-ocean (d/defaults base)
      base)))

(defn node
  "Selecting a node to create the instance under:

    (create <hypervisor> (node :remote-node) ....)
  "
  [n]
  (fn [instance]
    (assoc-in instance [(figure-virt instance) :node] n)))

(defn local
  "Selecting localhost hypervisor node"
  []
  (node (c/get! :shared :presets :default :node)))

(def ^{:doc "Creating an lxc container (create lxc ...)"} lxc {:lxc {} :machine {}})

(def ^{:doc "Creating a kvm VM: (create kvm ...)"} kvm {:kvm {} :machine {}})

(def ^{:doc "Adding a physical machine: (add physical  ...)"} physical {:physical {:broadcast "0.0.0.0" :mac "00:00:00:00:00:00"} :machine {}})

(def ^{:doc "Creating a ec2 VM: (create ec2 ...)"} droplet {:digital-ocean {} :machine {}})

(defn dispoable-instance []
  [default-machine (local) (os :ubuntu-desktop-22.04) c4-large :disposable "A temporary sandbox"])

(defn refer-system-presets []
  (require '[re-core.presets.systems :as spc :refer [node lxc kvm droplet physical os ubuntu-22_04 defaults local default-machine with-host machine]]))
