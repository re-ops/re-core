(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.redis :refer (clear-all)]  
    [flatland.useful.map :refer (dissoc-in*)]
    [cemerick.friend :as friend]
    [celestial.fixtures :refer (redis-prox-spec redis-type is-type? user-quota redis-actions)]
    [celestial.persistency :as p])
  (:use midje.sweet))


(against-background 
     [(friend/current-authentication) => {:identity "admin", :roles #{:celestial.roles/admin}, :username "admin"}
      (p/get-user! "admin") => {:envs [:dev :qa :prod] :roles #{:celestial.roles/admin} :username "admin"}
      (p/get-user! "ronen") => {:envs [:dev :qa] :roles #{:celestial.roles/user} :username "ronen"} ]
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type)))]
    (fact "Persisting type and host sanity" :integration :redis :systems
      (let [id (p/add-system redis-prox-spec)] 
            (p/get-type "redis") => redis-type 
            (p/get-system id) => redis-prox-spec
            (provided
              (friend/current-authentication) => {:identity "admin", :roles #{:celestial.roles/admin}, :username "admin"})      
            )) 

    (fact "host update" :integration  :redis :systems
          (let [id (p/add-system redis-prox-spec)] 
            (p/update-system id (assoc redis-prox-spec :foo 2)) 
            (:foo (p/get-system id)) => 2))

    (fact "simple clone" :integration :redis :systems
          (let [id (p/add-system redis-prox-spec)
                cloned (p/clone-system id "foo")] 
            (p/get-system cloned) => 
            (-> redis-prox-spec 
                (dissoc-in* [:proxmox :vmid])
                (dissoc-in* [:machine :ip]) 
                (assoc-in [:machine :hostname] "foo"))))

    (fact "persmissionless access" :integration :redis :systems
        (let [id (p/add-system (assoc redis-prox-spec :env :prod))] 
            (p/get-system id) => (throws ExceptionInfo (is-type? :celestial.persistency/persmission-violation))
            (provided (friend/current-authentication) => {:username "ronen"} :times 1) 
            (p/update-system id {}) => (throws ExceptionInfo (is-type? :celestial.persistency/persmission-violation))
            (provided (friend/current-authentication) => {:username "ronen"} :times 1)
            (p/delete-system id) => (throws ExceptionInfo (is-type? :celestial.persistency/persmission-violation))
            (provided (friend/current-authentication) => {:username "ronen"} :times 1)
            (p/add-system {:env :prod}) => (throws ExceptionInfo (is-type? :celestial.persistency/persmission-violation))
            (provided (friend/current-authentication) => {:username "ronen"} :times 1)
          )))

    (fact "with persmission" :integration :redis :systems
        (let [id (p/add-system redis-prox-spec)] 
            (p/get-system id) => redis-prox-spec
            (provided (friend/current-authentication) => {:username "ronen"} :times 1) 
            (p/update-system id (assoc redis-prox-spec :cpus 20)) => (contains ["OK"])
            (provided (friend/current-authentication) => {:username "ronen"} :times 6)
            (p/delete-system id) => [1 1]
            (provided (friend/current-authentication) => {:username "ronen"} :times 3)
            (p/add-system redis-prox-spec) => truthy
            (provided (friend/current-authentication) => {:username "ronen"} :times 3)
          ))) 


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
          (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user)))))

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


