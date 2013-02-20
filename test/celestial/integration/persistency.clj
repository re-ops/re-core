(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:require 
    [celestial.persistency :as p])
  (:use clojure.test
        [celestial.redis :only (clear-all)]
        [celestial.common :only (slurp-edn)]
        ))


(deftest ^:redis sanity
  (let [t (slurp-edn "fixtures/redis-type.edn")
        m  (slurp-edn "fixtures/redis-system.edn")]
    (clear-all)
    (p/new-type "redis" t) 
    (p/register-host "redis.local" "redis" m) 
    (is (= (p/type "redis") t))
    (is (= (p/host "redis.local") {:machine m :type "redis"}))
    ) )
