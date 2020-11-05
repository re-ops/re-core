(ns re-core.presets.types
  "Type presets that enable us to create types quickly, for example:
    (create puppet :re-ops default-src \"re-ops instances\") ; default source convention from type name
    (create puppet :re-ops (src  \"re-ops instances\") \"re-ops instances\") ; source specified
  "
  (:require
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [re-core.specs :as core]
   [clojure.core.strint :refer (<<)]
   [re-core.repl.types :refer (prefix-key)]))

(def home (System/getProperty "user.home"))

(def cog {:cog {:args []}})

(defn src [s]
  "Set source code location"
  (fn [instance]
    (assoc-in instance [(prefix-key instance) :src] s)))

(defn default-src
  [instance]
  ((src (<< "~{home}/code/re-ops/re-cipes/resources/")) instance))

(defn args
  "Set script arguments"
  [& as]
  (fn [instance]
    (assoc-in instance [(prefix-key instance) :args] (into [] as))))

(defn with-plan [p]
  "Set script arguments"
  (fn [instance]
    (assoc-in instance [(prefix-key instance) :plan] p)))

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
        (symbol? a) (into-spec (assoc m :plan a) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn materialize-preset [base args]
  (let [{:keys [plan fns type description]} (into-spec {} args)
        transforms [(with-type type) (with-desc description) (with-plan plan)]]
    (reduce (fn [m f] (f m)) base (apply conj transforms fns))))

(defn validate
  "Validate materialized type using ::core/type specification.
   Failing results are converted into expound format"
  [type]
  (if (s/valid? ::core/type type)
    {true type}
    {false (expound/expound-str ::core/type type)}))

(defn refer-type-presets []
  (require '[re-core.presets.types :as tp :refer [cog src default-src args]]))
