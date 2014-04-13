(ns celestial.test.validations
 (:require
   [celestial.roles :refer (admin)]
   [celestial.persistency :refer (validate-type user-exists? validate-user)]
   [celestial.persistency.quotas :refer (validate-quota)]
   [aws.validations :as awsv]
   [physical.validations :as phv]
   [docker.validations :as dv]
   [celestial.fixtures.data :refer 
    (redis-type user-quota redis-ec2-spec redis-physical redis-docker-spec)]
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

(fact "non int limit quota" 
   (validate-quota (assoc-in user-quota [:quotas :dev :aws :limits :count] "1")) => 
      (throws ExceptionInfo (with-m? {:quotas {:dev {:aws {:limits {:count  "must be a integer"}}}}}))
   (provided (user-exists? "foo") => true :times 1))

(fact "user validation"
     (validate-user {:username "foo" :password "bar" :roles admin :envs []})  => truthy
     (validate-user {:password "bar" :roles admin :envs []})  => 
       (throws ExceptionInfo (with-m? {:username "must be present"}))

     (validate-user {:username "foo" :password "bar" :roles admin})  => 
       (throws ExceptionInfo (with-m? {:envs "must be present"}))

     (validate-user {:username "foo" :password "" :roles admin :envs []})  => 
      (throws ExceptionInfo (with-m? {:password "must be a non empty string"}))

     (validate-user {:username "foo" :password "bar" :roles admin :envs [""]})  => 
       (throws ExceptionInfo (with-m? {:envs '({0 "must be a keyword"})} ))

     (validate-user {:username "foo" :password "bar" :roles ["foo"] :envs []})  =>
       (throws ExceptionInfo 
               (with-m? {:roles '({0 "role must be either #{:celestial.roles/super-user :celestial.roles/anonymous :celestial.roles/user :celestial.roles/admin :celestial.roles/system}"})} ))
      )

(fact "aws entity validations" 
  (awsv/validate-entity redis-ec2-spec) => {}
  ; TODO looks like a subs issue
  #_(awsv/validate-entity  
    (merge-with merge redis-ec2-spec {:aws {:volumes [{:device "do"}]}} )) => {}

  (awsv/validate-entity 
    (merge-with merge redis-ec2-spec {:aws {:security-groups [1]}})) =>
    (throws ExceptionInfo (with-m? {:aws {:security-groups '({0 "must be a string"})}}))

  (awsv/validate-entity 
    (merge-with merge redis-ec2-spec {:aws {:availability-zone 1}})) =>
    (throws ExceptionInfo (with-m? {:aws {:availability-zone "must be a string"}})))

(fact "aws provider validation"
  (let [base {:aws {:instance-type "m1.small" :key-name "foo" :min-count 1 :max-count 1}}]
   
   (awsv/provider-validation base) => {}

   (awsv/provider-validation 
     (merge-with merge base {:aws {:placement {:availability-zone "eu-west-1a"}}})) => {}

   (awsv/provider-validation 
     (merge-with merge base {:aws {:placement {:availability-zone 1}}})) => 
    (throws ExceptionInfo 
      (with-m? {:placement {:availability-zone "must be a string"}})))
      
      )


(fact "physical systmes validation" 
   (phv/validate-entity redis-physical) => {}

   (phv/validate-entity (assoc-in redis-physical [:physical :mac] "aa:bb")) =>
     (throws ExceptionInfo (with-m? {:physical {:mac "must be a legal mac address"}}))

   (phv/validate-entity (assoc-in redis-physical [:physical :broadcast] "a.1.2")) =>
      (throws ExceptionInfo (with-m? {:physical {:broadcast "must be a legal ip address"}}))

)

(fact "docker systems validation" 
   (dv/validate-entity redis-docker-spec) => {})

