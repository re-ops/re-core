(ns re-core.test.physical
  "Physical instance creation"
  (:import clojure.lang.ExceptionInfo)
  (:require
   physical.provider
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.model :refer (vconstruct)]
   [re-core.fixtures.data :refer (redis-physical)])
  (:use clojure.test))

(deftest instance-creation
  (is (not (nil? (vconstruct redis-physical))))
  (is (thrown? ExceptionInfo (vconstruct (dissoc-in* redis-physical [:physical :mac])))))
