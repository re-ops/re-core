(ns re-core.test.physical
  "Physical instance creation"
  (:import clojure.lang.ExceptionInfo)
  (:require
    physical.provider 
    [flatland.useful.map :refer (dissoc-in*)]
    [re-core.fixtures.core :refer (with-m?)] 
    [re-core.model :refer (vconstruct)] 
    [re-core.fixtures.data :refer (redis-physical)]) 
  (:use midje.sweet))


(fact "basic physical machine instance creation" 
  (vconstruct redis-physical) => truthy
  (vconstruct (dissoc-in* redis-physical [:physical :mac])) => 
    (throws ExceptionInfo (with-m? {:interface {:mac "must be present"}})))
