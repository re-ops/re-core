(ns celestial.integration.persmissions
  "System access permissions and envs"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [flatland.useful.map :refer (dissoc-in*)]
    [cemerick.friend :as friend]
    [celestial.redis :refer (clear-all)]  
    [celestial.fixtures :refer (redis-prox-spec redis-type is-type? with-conf add-users)]
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s] 
    )
  (:use midje.sweet))

(with-conf
  (against-background 
    [(friend/current-authentication) => {:identity "admin", :roles #{:celestial.roles/admin}, :username "admin"}]
    (with-state-changes [(before :facts (do (clear-all) (add-users) (p/add-type redis-type)))]
      (fact "type and host sanity" :integration :redis :systems
            (let [id (s/add-system redis-prox-spec)] 
              (p/get-type "redis") => redis-type 
              (s/get-system id) => redis-prox-spec
              (provided
                (friend/current-authentication) => {:identity "admin", :roles #{:celestial.roles/admin} :username "admin"})))

      (fact "host update" :integration  :redis :systems
            (let [id (s/add-system redis-prox-spec)] 
              (s/update-system id (assoc redis-prox-spec :foo 2)) 
              (:foo (s/get-system id)) => 2))

      (fact "simple clone" :integration :redis :systems
            (let [id (s/add-system redis-prox-spec)
                  cloned (s/clone-system id "foo")] 
              (s/get-system cloned) => 
              (-> redis-prox-spec 
                  (dissoc-in* [:proxmox :vmid])
                  (dissoc-in* [:machine :ip]) 
                  (assoc-in [:machine :hostname] "foo"))))

      (fact "persmissionless access" :integration :redis :systems
            (let [id (s/add-system (assoc redis-prox-spec :env :prod))] 
              (s/get-system id) => 
                 (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (friend/current-authentication) => {:username "ronen"} :times 1) 
              (s/update-system id {}) => 
                (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (friend/current-authentication) => {:username "ronen"} :times 1)
              (s/delete-system id) => 
                (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (friend/current-authentication) => {:username "ronen"} :times 1)
              (s/add-system {:env :prod}) => 
                (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (friend/current-authentication) => {:username "ronen"} :times 1)
              ))

      (fact "with persmission" :integration :redis :systems
            (let [id (s/add-system redis-prox-spec)] 
              (s/get-system id) => redis-prox-spec
              (provided (friend/current-authentication) => {:username "ronen"} :times 1) 
              (s/update-system id (assoc redis-prox-spec :cpus 20)) => (contains ["OK"])
              (provided (friend/current-authentication) => {:username "ronen"} :times 6)
              (s/delete-system id) => [1 1 1]
              (provided (friend/current-authentication) => {:username "ronen"} :times 3)
              (s/add-system redis-prox-spec) => truthy
              (provided (friend/current-authentication) => {:username "ronen"} :times 3))))))
