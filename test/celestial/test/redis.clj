(ns celestial.test.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use 
    clojure.test 
    [celestial.redis :only (lock release)]))


(deftest ^:redis basic-locking 
  (is (lock 1 200)) 
  (is (not (lock 1 200))) 
  (Thread/sleep 200)
  (is (lock 1 200)))


(deftest ^:redis releasing  
  )
