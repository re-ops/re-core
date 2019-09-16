(ns re-core.presets.systems
  "Common preset functions"
  (:require
   [expound.alpha :as expound]
   [re-core.specs :as core]
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
        (string? a) (into-spec (assoc m :hostname a) (rest args))
        (number? a) (into-spec (assoc m :total a) (rest args))
        (keyword? a) (into-spec (assoc m :type a) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn with-type [t]
  (fn [instance] (assoc instance :type t)))

(defn with-host [h]
  (fn [{:keys [type] :as instance}]
    (assoc-in instance [:machine :hostname] (or h (name type)))))

(defn materialize-preset
  "Convert a preset into a system using provided args (functions, keyswords etc..)"
  [base args]
  (let [{:keys [fns total type hostname]} (into-spec {} args)
        transforms [(with-type type) (with-host hostname) name-gen]
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

(def ubuntu-18_04_3 (os :ubuntu-18.04.3))

(defn machine [user domain]
  (fn [instance]
    (update instance :machine
            (fn [m] (merge m {:user user :domain domain})))))

(def default-machine (machine "re-ops" "local"))

(defn defaults
  "Applying default setting for our preset check the matching aws/defaults and digital-ocean/defaults for details per hypervisor information"
  [instance]
  (let [base (-> instance (ubuntu-18_04_3) (default-machine))]
    (case (figure-virt base)
      :digital-ocean (d/defaults base)
      :aws (amz/defaults base)
      base)))

(defn node [n]
  (fn [instance]
    (assoc-in instance [(figure-virt instance) :node] n)))

(def local (node :localhost))

(def lxc {:lxc {} :machine {}})

(def kvm {:kvm {} :machine {}})

(def ec2 {:aws {} :machine {}})

(def droplet {:digital-ocean {} :machine {}})

(defn refer-system-presets []
  (require '[re-core.presets.systems :as spc :refer [node lxc kvm droplet ec2 os ubuntu-18_04_3 defaults local default-machine]]))
