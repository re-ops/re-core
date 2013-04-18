(ns celestial.integration.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use midje.sweet
        [celestial.fixtures :only (is-type?)]
        [celestial.redis :only (acquire release get- with-lock synched-map clear-all clear-locks)])
  (:import clojure.lang.ExceptionInfo))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic locks in redis" :integration :redis 
        (acquire 2) => truthy ; for 2 sec
        (acquire 2 {:wait-time 200}) => falsey ; too early
        (Thread/sleep 1000)
        (acquire 2) => truthy)

  (fact "releasing locks" :integration :redis 
        (let [uuid (acquire 3)]
          (acquire 3 {:wait-time 200}) => falsey ; too early
          (release 3 uuid) => truthy
          (acquire 3) => truthy)) 

  (fact "trying to release already released locks" :integration :redis 
        (let [uuid (acquire 4 {:wait-time 2000}) f1 (future (release 4 uuid))]
          (Thread/sleep 200); letting future run 
          (release 4 uuid) => falsey)) 

  (fact "locking scope" :integration :redis 
        (try 
          (with-lock 5 #(throw (Exception.)))
          (catch Exception e nil))
        (get- 5) => falsey) 

  (fact "locking failure" :integration :redis 
        (acquire 6 {:expiry 3}) 
        (with-lock 6 #() {:wait-time 10}) => (throws ExceptionInfo (is-type? :celestial.redis/lock-fail)))

  (fact "using lock expiry" :integration :redis 
        (future (with-lock 9 (fn [] (Thread/sleep 1000)) {:expiry 4}))
        (Thread/sleep 200)
        (with-lock 9 #() {:expiry 3 :wait-time 10}) =>
           (throws ExceptionInfo (is-type? :celestial.redis/lock-fail)))

  (fact "clearing locks" :integration :redis locks-clearence 
        (acquire 7 {:expiry 3}) 
        (clear-locks) 
        (acquire 7 {:expiry 3}) => truthy) 

  (fact "synched" :integration :redis 
        (let [ids (synched-map :ids)]
          (deref ids) => {} 
          (swap! ids assoc :foo 1)    
          (swap! ids assoc :bar 2)    
          (deref (synched-map :ids)) => {:bar 2 :foo 1}
          (reset! ids {:foo {:bar 2}})
          (deref (synched-map :ids)) => {:foo {:bar 2}}
          (reset! ids nil) => (throws java.lang.AssertionError)
          (deref (synched-map :ids)) => {:foo {:bar 2}}))) 
