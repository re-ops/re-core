(ns celestial.test.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use 
    clojure.test 
    [celestial.redis :only (acquire release)]))


(deftest ^:redis basic-locking 
  (is (acquire 2)) ; for 2 sec
  (is (not (acquire 2 {:wait-time 200}))) ; too early
  (Thread/sleep 1000)
  (is (acquire 2)))

(deftest ^:redis releasing  
  (let [uuid (acquire 3)]
    (is (not (acquire 3 {:wait-time 200}))) ; too early
    (is (release 3 uuid)) 
    (is (acquire 3))))

(deftest ^:redis aleady-released
  (let [uuid (acquire 4 {:wait-time 2000}) f1 (future (release 4 uuid))]
     (Thread/sleep 200); letting future run 
     (is (not (release 4 uuid)))))
