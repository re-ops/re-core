(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p])
  (:use 
    midje.sweet
    [celestial.fixtures :only (redis-prox-spec redis-type)]
    [celestial.redis :only (clear-all)]))


(with-state-changes [(before :facts (clear-all))]
  (fact "Persisting type and host sanity" :integration :redis 
        (p/new-type "redis" redis-type) 
        (p/register-host redis-prox-spec) 
        (p/type-of "redis") => redis-type
        (p/host "red1") => redis-prox-spec)

  (fact "fuzzy host lookup" :integration :redis 
        (p/new-type "redis" redis-type) 
        (p/register-host redis-prox-spec) 
        (p/fuzzy-host "red1") => redis-prox-spec) 

  (fact "host update" :integration  :redis 
        (p/new-type "redis" redis-type) 
        (p/register-host redis-prox-spec) 
        (p/update-host "red1" {:foo 2})
        (:foo (p/host "red1")) => 2)) 
