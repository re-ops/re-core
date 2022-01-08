(ns re-core.persistency.common
  "common persistency layer functions"
  (:require
   [clojure.walk :refer [postwalk]]
   [com.rpl.specter :refer (transform select ALL)]
   [re-cog.facts.datalog :refer (flatten-keys fact-pairs)]
   [clojure.string :refer (escape join)]))

(defn args-of
  "grab args from string"
  [s]
  (into #{} (map #(escape % {\~ "" \{ "" \} ""}) (re-seq #"~\{\w+\}" s))))

(defn with-transform [t f & [a1 a2 & r :as args]]
  (cond
    (map? a1) (apply f (t a1) r)
    (map? a2) (apply f a1 (t a2) r)
    :else (apply f args)))

(defn into-vec [m]
  (mapv m (sort (keys m))))

(defn vec-map? [m]
  (every? (partial re-find #"\d+") (map name (keys m))))

(defn restore-vectors [m]
  (postwalk
   (fn [v]
     (if (and (map? v) (vec-map? v)) (into-vec v) v)) m))

(defn unflatten [m]
  (restore-vectors
   (reduce
    (fn [m [k v]]
      (assoc-in m (mapv keyword (clojure.string/split (subs (str k) 1) #"\/")) v)) {} m)))

(defn join-keys [[ks v]]
  [(keyword (join "/" (map (fn [k] (if (keyword? k) (name k) (str k))) ks))) v])

(defn flatten- [m]
  (into {} (map join-keys (flatten-keys m))))
