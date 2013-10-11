(ns celestial.integration.persmissions
  "System access permissions and envs"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [flatland.useful.map :refer (dissoc-in*)]
    [cemerick.friend :as friend]
    [celestial.redis :refer (clear-all)]  
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
            ))

    (fact "with persmission" :integration :redis :systems
          (let [id (p/add-system redis-prox-spec)] 
            (p/get-system id) => redis-prox-spec
            (provided (friend/current-authentication) => {:username "ronen"} :times 1) 
            (p/update-system id (assoc redis-prox-spec :cpus 20)) => (contains ["OK"])
            (provided (friend/current-authentication) => {:username "ronen"} :times 6)
            (p/delete-system id) => [1 1 1]
            (provided (friend/current-authentication) => {:username "ronen"} :times 3)
            (p/add-system redis-prox-spec) => truthy
            (provided (friend/current-authentication) => {:username "ronen"} :times 3)))
    
    (fact "Filtering systems per user" :integration :redis :systems
           (p/add-system (assoc redis-prox-spec :env :prod)) => truthy
           (provided (friend/current-authentication) => {:username "admin"} :times 3) 
           (p/add-system (assoc redis-prox-spec :env :dev)) => truthy
           (provided (friend/current-authentication) => {:username "ronen"} :times 3)
           ;; (count (p/all-systems)) => 1
            )

    ))
