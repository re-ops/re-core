(ns re-core.presets.common
  "Common preset functions"
  (:require
   [re-core.common :refer (gen-uuid)]))

(defn os [k]
  (fn [instance]
    (assoc-in instance [:machine :os] k)))

(defn machine [user domain os]
  {:user user :domain domain :os os})

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

(defn with-type [t]
  (fn [instance] (assoc instance :type t)))

(defn with-host [h]
  (fn [{:keys [type] :as instance}]
    (assoc-in instance [:machine :hostname] (or h (name type)))))

(defn refer-common-presets []
  (require '[re-core.presets.common :as spc :refer [os]]))
