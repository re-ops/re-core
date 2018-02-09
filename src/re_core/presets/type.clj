(ns re-core.presets.type
  "Type presets"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-core.common :refer (gen-uuid)]))

(def home (System/getProperty "user.home"))

(def puppet {:puppet {:tar "" :args []}})

(defn src [{:keys [type] :as instance}]
  (assoc-in instance [:puppet :src] (<< "~{home}/code/boxes/~{type}-sandbox/")))

(defn with-type [t]
  (fn [instance]
    (assoc instance :type t)))

(defn with-desc [d]
  (fn [instance] (assoc instance :description d)))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args) {:keys [fns]} m]
      (cond
        (string? a) (into-spec (assoc m :description a) (rest args))
        (keyword? a) (into-spec (assoc m :type (name a)) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn refer-type-presets []
  (require '[re-core.presets.type :as tp :refer [puppet src]]))