(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p])
  (:use clojure.test
    [celestial.fixtures :only (spec type)]
    [celestial.redis :only (clear-all)]))


(deftest ^:redis sanity
    (clear-all)
    (p/new-type "redis" type) 
    (p/register-host "redis.local" "redis" spec) 
    (is (= (p/type "redis") type))
    (is (= (p/host "redis.local") {:machine spec :type "redis"}))
    )

(deftest ^:redis fuzzy-lookup 
    (clear-all)
    (p/new-type "redis" type) 
    (p/register-host "redis" "redis" spec) 
    (is (= (p/fuzzy-host "redis.local") {:machine spec :type "redis"})))

