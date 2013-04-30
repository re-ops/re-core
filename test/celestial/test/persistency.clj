(ns celestial.test.persistency
 (:use 
   midje.sweet 
   [celestial.persistency :only (validate-type)]
   [celestial.fixtures :only (redis-type is-type?)]
   ))

(fact "puppet std type validation"
    
    (validate-type redis-type) => true

    (validate-type (assoc-in redis-type [:puppet-std :module :src] nil)) => 
       (throws clojure.lang.ExceptionInfo (is-type? :celestial.persistency/non-valid-type))

    (validate-type (dissoc redis-type :classes)) => 
       (throws clojure.lang.ExceptionInfo (is-type? :celestial.persistency/non-valid-type)))


(fact "non puppet type"
  (validate-type {:type "foo"}) => true)
