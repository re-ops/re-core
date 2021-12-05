(ns re-core.test.specs
  (:require
   [re-core.persistency.types :refer (exists?)]
   [re-core.specs :as core]
   [re-core.fixtures.data :as d]
   [clojure.spec.alpha :as s :refer (valid?)])
  (:use clojure.test))

(defn valid
  "Since we don't populate the type we expect it will be missing"
  [system]
  (let [problems ((s/explain-data ::core/system system) :clojure.spec.alpha/problems)
        {:keys [path pred]} (first problems)]
    (and (= (count problems) 1) (= path [:type]) (= pred 're-core.persistency.types/exists?))))

(deftest legal-systems
  (is (valid d/redis-digital))
  (is (valid d/redis-lxc))
  (is (valid d/redis-physical))
  (is (valid d/redis-kvm)))
