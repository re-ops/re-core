(ns re-core.persistency.common
  "common persistency layer functions"
  (:require
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
