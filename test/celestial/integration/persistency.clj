(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.persistency :as p])
  (:use 
    midje.sweet
    [celestial.fixtures :only (redis-prox-spec redis-type is-type? user-quota)]
    [celestial.redis :only (clear-all)]))


(with-state-changes [(before :facts (clear-all))]
  (fact "Persisting type and host sanity" :integration :redis 
        (p/add-type redis-type) 
        (let [id (p/add-system redis-prox-spec)] 
          (p/get-type "redis") => redis-type 
          (p/get-system id) => redis-prox-spec)) 

  (fact "host update" :integration  :redis 
        (p/add-type redis-type) 
        (let [id (p/add-system redis-prox-spec)] 
          (p/update-system id (assoc redis-prox-spec :foo 2)) 
          (:foo (p/get-system id)) => 2))) 

(with-state-changes [(before :facts (clear-all))]
  (fact "generated crud user ops" :integration :redis
        (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user}} id (p/add-user user)]
          (p/get-user id) => user
          (p/user-exists? id) => truthy
          (p/update-user (merge user {:username "foo" :password "123"}))
          (p/get-user id) => (merge user {:username "foo" :password "123"})
          (p/delete-user id)
          (p/user-exists? id) => falsey
          ))

  (fact "non valid user" :integration :redis
        (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user}} id (p/add-user user)]
          (p/add-user (dissoc user :username)) => 
          (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user))
          (p/update-user (dissoc user :username)) =>
          (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user)))))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic quota usage" :integration :redis :quota
        (with-redefs [p/curr-user (fn [] "foo")]
          (p/add-user {:username "foo" :password "bla" :roles {}})
          (p/add-quota (assoc-in user-quota [:quotas :proxmox :used] nil)) 
          (p/increase-use 1 redis-prox-spec) => nil 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}}) 
          (p/increase-use 2  redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 2}}}) 
          (p/with-quota (fn [] 1) redis-prox-spec) => (throws ExceptionInfo (is-type? :celestial.persistency/quota-limit-reached)) 
          (p/decrease-use 2 redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}})  
          (p/increase-use 3  redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 3}}})) 
        )) 
