(ns re-core.presets.type
  "Type presets that enable us to create types quickly, for example:
    (create puppet :re-ops default-src \"re-ops instances\") ; default source convention from type name
    (create puppet :re-ops (src  \"re-ops instances\") \"re-ops instances\") ; source specified
  "
  (:require
   [clojure.core.strint :refer (<<)]
   [re-core.common :refer (gen-uuid)]))

(def home (System/getProperty "user.home"))

(def puppet {:puppet {:tar "" :args []}})
(def reconf {:re-conf {:args []}})

(defn prefix-key [m]
  (first (filter #{:puppet :re-conf} (keys m))))

(defn src [s]
  "Set source code location"
  (fn [instance]
    (assoc-in instance [(prefix-key instance) :src] s)))

(defn default-src
  "Using name as a convention source location"
  [instance]
  ((src (<< "~{home}/code/boxes/~{type}-sandbox/")) instance))

(defn args
  "Set script arguments"
  [& as]
  (fn [instance]
    (assoc-in instance [(prefix-key instance) :args] (into [] as))))

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
  (require '[re-core.presets.type :as tp :refer [puppet reconf src default-src args]]))
