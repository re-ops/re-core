(ns celestial.integration.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use clojure.test slingshot.test
    [celestial.redis :only (acquire release get- with-lock synched-map clear-all)])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once (fn [f] (f) (clear-all)))

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

(deftest ^:redis locking-scope
  (try 
    (with-lock 5 #(throw (Exception.)))
    (catch Exception e nil))
  (is (not (get- 5))))

(deftest ^:redis locking-failure 
  (acquire 6 {:expiry 3}) 
  (is (thrown+? [:type :celestial.redis/lock-fail] (with-lock 6 #() {:wait-time 10}))))


(deftest ^:redis with-lock-expiry
 (future (with-lock 9 (fn [] (Thread/sleep 1000)) {:expiry 4}))
 (Thread/sleep 200)
 (is (thrown+? [:type :celestial.redis/lock-fail] 
        (with-lock 9 #() {:expiry 3 :wait-time 10}))))

(deftest ^:redis synched
   (let [ids (synched-map :ids)]
      (is (= (deref ids) {}))
      (swap! ids assoc :foo 1)    
      (swap! ids assoc :bar 2)    
      (is (= (deref (synched-map :ids)) {:bar 2 :foo 1}))
      (reset! ids {:foo {:bar 2}})
      (is (= (deref (synched-map :ids)) {:foo {:bar 2}}))
      (is (thrown? java.lang.AssertionError (reset! ids nil)))
      (is (= (deref (synched-map :ids)) {:foo {:bar 2}}))
    ))
