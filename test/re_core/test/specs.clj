(ns re-core.test.specs
  (:require
   [re-core.specs :as core]
   [re-core.fixtures.data :as d]
   [clojure.spec.alpha :as s :refer (valid?)])
  (:use clojure.test))

(deftest legal-systems
  (is (valid? ::core/system d/redis-digital))
  (is (valid? ::core/system d/redis-lxc))
  (is (valid? ::core/system d/redis-physical))
  (is (valid? ::core/system d/redis-kvm)))
