(ns re-core.presets.common
  "Common preset functions"
  (:require
   [expound.alpha :as expound]
   [re-core.specs :as core]
   [clojure.spec.alpha :as s]
   [re-core.model :refer (figure-virt)]
   [re-core.common :refer (gen-uuid)]))

(defn name-gen
  "Generating a unique hostname from host/type + uuid"
  [instance]
  (update-in instance [:machine :hostname]
             (fn [hostname] (str hostname "-" (.substring (gen-uuid) 0 10)))))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args) {:keys [fns]} m]
      (cond
        (string? a) (into-spec (assoc m :hostname a) (rest args))
        (number? a) (into-spec (assoc m :total a) (rest args))
        (keyword? a) (into-spec (assoc m :type a) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn validate [spec]
  (if-not (s/valid? ::core/system spec)
    (expound/expound ::core/system spec)
    spec))

(defn with-type [t]
  (fn [instance] (assoc instance :type t)))

(defn with-host [h]
  (fn [{:keys [type] :as instance}]
    (assoc-in instance [:machine :hostname] (or h (name type)))))

(defn into-specs [base args]
  (let [{:keys [fns total type hostname]} (into-spec {} args)
        transforms [(with-type type) (with-host hostname) name-gen]
        all (apply conj transforms fns)]
    (map
     (fn [_] (validate (reduce (fn [m f] (f m)) base all))) (range (or total 1)))))

(defn os [k]
  (fn [instance]
    (assoc-in instance [:machine :os] k)))

(def ubuntu-18_04_2 (os :ubuntu-18.04.2))

(defn machine [user domain]
  (fn [instance]
    (update instance :machine
            (fn [m] (merge m {:user user :domain domain})))))

(def default-machine (machine "re-ops" "local"))

(defn defaults
  "default machine and os settings"
  [instance]
  (-> instance (ubuntu-18_04_2) (default-machine)))

(defn node [n]
  (fn [instance] (assoc-in instance [(figure-virt instance) :node] n)))
(def local (node :localhost))

(def lxc {:lxc {} :machine {}})

(def kvm {:kvm {} :machine {}})

(defn refer-common-presets []
  (require '[re-core.presets.common :as spc :refer [node lxc kvm os ubuntu-18_04_2 defaults local]]))
