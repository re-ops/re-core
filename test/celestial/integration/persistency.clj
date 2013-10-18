(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.redis :refer (clear-all)]  
    [flatland.useful.map :refer (dissoc-in*)]
    [celestial.fixtures.core :refer (with-conf is-type?)]
    [celestial.fixtures.data :refer (redis-prox-spec redis-type user-quota redis-actions)]
    [celestial.persistency :as p])
  (:use midje.sweet))


(with-conf
  (with-state-changes [(before :facts (clear-all))]
    (fact "generated crud user ops" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (p/add-user user)]
            (p/get-user id) => user
            (p/user-exists? id) => truthy
            (p/update-user (merge user {:username "foo" :password "123"}))
            (p/get-user id) => (merge user {:username "foo" :password "123"})
            (p/delete-user id)
            (p/user-exists? id) => falsey))

    (fact "non valid user" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (p/add-user user)]
            (p/add-user (dissoc user :username)) => 
            (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user))
            (p/update-user (dissoc user :username)) =>
            (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user))))))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic quota usage" :integration :redis :quota
        (with-redefs [p/curr-user (fn [] "foo")]
          (p/add-user {:username "foo" :password "bla" :roles {} :envs []})
          (p/add-quota (assoc-in user-quota [:quotas :proxmox :used] nil)) 
          (p/increase-use 1 redis-prox-spec) => "OK"
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}}) 
          (p/increase-use 2  redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 2}}}) 
          (p/with-quota (fn [] 1) redis-prox-spec) => (throws ExceptionInfo (is-type? :celestial.persistency/quota-limit-reached)) 
          (p/decrease-use 2 redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}})  
          (p/increase-use 3  redis-prox-spec) 
          (:quotas (p/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 3}}})) 
        )) 

(with-state-changes [(before :facts (clear-all))]
  (fact "basic actions usage" :integration :redis :actions
        (p/add-type redis-type) 
        (let [id (p/add-action redis-actions)]
          (p/get-action id) => redis-actions
          (p/get-action-index :operates-on "redis") => [(str id)]
          (p/find-action-for :deploy "redis") => redis-actions
          )))


