(ns celestial.fixtures
  (:refer-clojure :exclude [type])
  (:use [celestial.common :only (slurp-edn)]))

(def spec (slurp-edn "fixtures/redis-system.edn"))

(def type (slurp-edn "fixtures/redis-type.edn"))

(defn alter-confi []
   (alter-var-root)
  )
