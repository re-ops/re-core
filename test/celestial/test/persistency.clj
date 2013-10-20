(ns celestial.test.persistency
 (:require
   [celestial.roles :refer (admin)]
   [celestial.persistency :refer (validate-type validate-quota user-exists? validate-user)]
   [aws.validations :as awsv]
   [celestial.fixtures.data :refer (redis-type user-quota redis-ec2-spec)]
   [celestial.fixtures.core :refer (is-type? with-m?)])
 (:use midje.sweet)
 (:import clojure.lang.ExceptionInfo))

(fact "puppet std type validation"
    
    (validate-type redis-type) => truthy

    (validate-type (assoc-in redis-type [:puppet-std :module :src] nil)) => 
       (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-type))

    (validate-type (assoc-in redis-type [:puppet-std :args] nil)) => truthy 

    (validate-type (assoc-in redis-type [:puppet-std :args] [])) => truthy 
    
    (validate-type (assoc-in redis-type [:puppet-std :args] {})) => 
       (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-type))
      
    (validate-type (dissoc redis-type :classes)) => 
       (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-type)))

(fact "non puppet type"
  (validate-type {:type "foo"}) => truthy)

(fact "quotas validations"
     (validate-quota user-quota) => truthy
     (provided (user-exists? "foo") => true :times 1))

(fact "non int limit quota" filters
   (validate-quota (assoc-in user-quota [:quotas :aws :limit] "1")) => 
      (throws ExceptionInfo (with-m? {:quotas '(({:aws {:limit ("must be a integer")}}))}))
   (provided (user-exists? "foo") => true :times 1))

(fact "user validation"
     (validate-user {:username "foo" :password "bar" :roles admin :envs []})  => truthy
     (validate-user {:password "bar" :roles admin :envs []})  => 
       (throws ExceptionInfo (with-m? {:username '("must be present")}))
     (validate-user {:username "foo" :password "bar" :roles admin})  => 
       (throws ExceptionInfo (with-m? {:envs '("must be present")}))
     (validate-user {:username "foo" :password "bar" :roles admin :envs [""]})  => 
       (throws ExceptionInfo (with-m? {:envs '(({0 ("must be a keyword")}))} ))
     (validate-user {:username "foo" :password "bar" :roles ["foo"] :envs []})  =>
       (throws ExceptionInfo (with-m? {:roles '(({0 ("role must be either #{:celestial.roles/anonymous :celestial.roles/user :celestial.roles/admin}")}))} ))
      )

(fact "aws volumes validations" 
  (awsv/validate-entity redis-ec2-spec) => {}
  #_(awsv/validate-entity  
    (merge-with merge redis-ec2-spec {:aws {:volumes [{:device "do"}]}} )) => {}
  )
