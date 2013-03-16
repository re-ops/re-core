(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p])
  (:use clojure.test
    [celestial.fixtures :only (redis-prox-spec redis-type)]
    [celestial.redis :only (clear-all)]))


(use-fixtures :each (fn [f] (clear-all) (f)))

(deftest ^:redis sanity
    (p/new-type "redis" redis-type) 
    (p/register-host redis-prox-spec) 
    (is (= (p/type-of "redis") redis-type))
    (is (= (p/host "red1") redis-prox-spec)))

(deftest ^:redis fuzzy-lookup 
    (p/new-type "redis" redis-type) 
    (p/register-host redis-prox-spec) 
    (is (= (p/fuzzy-host "red1") redis-prox-spec)))

(deftest ^:redis host-update 
   (p/new-type "redis" redis-type) 
   (p/register-host redis-prox-spec) 
   (p/update-host "red1" {:foo 2})
   (is (= (:foo (p/host "red1")) 2))
  )
