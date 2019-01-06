(ns re-core.test.digital
  (:require
   [re-core.model :refer (vconstruct)]
   [re-core.fixtures.data :refer [redis-digital]])
  (:use clojure.test))

(deftest digital-vconstruct
  (let [{:keys [machine digital-ocean]} redis-digital]
    (testing "legal digital-ocean system"
      (is (= (get-in (vconstruct redis-digital) [:drp :name]) "red1.local")))))
