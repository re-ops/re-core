(ns celestial.test.persistency
 (:use 
   midje.sweet 
   [celestial.persistency :only (validate-type validate-quota user-exists?)]
   [celestial.fixtures :only (redis-type is-type? user-quota)]
   ))

(fact "puppet std type validation"
    
    (validate-type redis-type) => true

    (validate-type (assoc-in redis-type [:puppet-std :module :src] nil)) => 
       (throws clojure.lang.ExceptionInfo (is-type? :celestial.persistency/non-valid-type))

    (validate-type (assoc-in redis-type [:puppet-std :args] nil)) => truthy 
    (validate-type (assoc-in redis-type [:puppet-std :args] [])) => truthy 
    
    (validate-type (assoc-in redis-type [:puppet-std :args] {})) => 
       (throws clojure.lang.ExceptionInfo (is-type? :celestial.persistency/non-valid-type))
      

    (validate-type (dissoc redis-type :classes)) => 
       (throws clojure.lang.ExceptionInfo (is-type? :celestial.persistency/non-valid-type)))

(fact "non puppet type"
  (validate-type {:type "foo"}) => true)

(fact "quotas validations"
     (validate-quota user-quota) => true
     (provided (user-exists? "foo") => true :times 1))


