(ns celestial.integration.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use midje.sweet
        [celestial.fixtures :only (is-type?)]
        [celestial.redis :only (get- synched-map clear-all clear-locks)])
  (:import clojure.lang.ExceptionInfo))

(with-state-changes [(before :facts (clear-all))]
  (fact "synched" :integration :redis 
    (let [ids (synched-map :ids)]
       (deref ids) => {} 
       (swap! ids assoc :foo 1)    
       (swap! ids assoc :bar 2)    
       (deref (synched-map :ids)) => {:bar 2 :foo 1}
       (reset! ids {:foo {:bar 2}})
       (deref (synched-map :ids)) => {:foo {:bar 2}}
       (reset! ids nil) => (throws java.lang.AssertionError)
       (deref (synched-map :ids)) => {:foo {:bar 2}}
     ))) 
