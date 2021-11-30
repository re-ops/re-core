(ns re-core.persistency.common
  "common persistency layer functions"
  (:require
   [re-cog.facts.datalog :refer (flatten-keys join-keys fact-pairs)]
   [clojure.string :refer (escape)]))

(defn args-of
  "grab args from string"
  [s]
  (into #{} (map #(escape % {\~ "" \{ "" \} ""}) (re-seq #"~\{\w+\}" s))))

(defn with-transform [t f & [a1 a2 & r :as args]]
  (cond
    (map? a1) (apply f (t a1) r)
    (map? a2) (apply f a1 (t a2) r)
    :else (apply f args)))

(defn unflatten [m]
  (reduce
   (fn [m [k v]]
     (assoc-in m (mapv keyword (clojure.string/split (subs (str k) 1) #"\/")) v)) {} m))

(defn flatten- [m]
  (into {} (map first (map join-keys (flatten-keys m)))))

