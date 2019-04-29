(ns re-core.test.specs
  (:require
   re-core.specs
   [re-core.fixtures.data :as d]
   [clojure.spec.alpha :refer (valid?)])
  (:use clojure.test))

(deftest legal-systems
  (is (valid? :re-core/system d/redis-digital))
  (is (valid? :re-core/system d/redis-lxc))
  (is (valid? :re-core/system d/redis-physical))
  (is (valid? :re-core/system d/redis-kvm)))
