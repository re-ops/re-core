(ns celestial.integration.persistency.persmissions
  "System access permissions and envs"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [flatland.useful.map :refer (dissoc-in*)]
    [celestial.security :refer (current-user)]
    [celestial.redis :refer (clear-all)]  
    [celestial.fixtures.data :refer (redis-prox-spec redis-type)]
    [celestial.fixtures.core :refer (is-type? with-conf)]
    [celestial.fixtures.populate :refer (add-users)]
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s])
  (:use midje.sweet))

(with-conf
  (against-background 
    [(current-user) => {:identity "admin", :roles #{:celestial.roles/admin}, :username "admin"}]
    (with-state-changes [(before :facts (do (clear-all) (add-users) (p/add-type redis-type)))]
      (fact "type and host sanity" :integration :redis :systems
            (let [id (s/add-system redis-prox-spec)] 
              (p/get-type "redis") => redis-type 
              (s/get-system id) => redis-prox-spec
              (provided
                (current-user) => {:identity "admin", :roles #{:celestial.roles/admin} :username "admin"})))

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
              ; note that the assert-access precondition adds a current-user call
              (provided (current-user) => {:username "ronen"} :times 2) 
              (s/update-system id redis-prox-spec) => 
                 (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (current-user) => {:username "ronen"} :times 4)
              (s/delete-system id) => 
                  (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (current-user) => {:username "ronen"} :times 2)
              (s/add-system {:env :prod}) => 
                (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
              (provided (current-user) => {:username "ronen"} :times 2)
              ))

      (fact "with persmission" :integration :redis :systems
        (let [id (s/add-system redis-prox-spec)] 
          (s/get-system id) => redis-prox-spec
          (provided (current-user) => {:username "ronen"} :times 2) 
          (s/update-system id (assoc redis-prox-spec :cpus 20)) => (contains ["OK"])
          (provided (current-user) => {:username "ronen"} :times 12)
          (s/delete-system id) => [1 1 1]
          (provided (current-user) => {:username "ronen"} :times 6)
          (s/add-system redis-prox-spec) => truthy
          (provided (current-user) => {:username "ronen"} :times 6)))
      
      (fact "user trying to create on another username" :integration :redis :systems
         (s/add-system (assoc redis-prox-spec :user "foo")) =>
           (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation))
         (provided 
           (current-user) => {:username "ronen"} :times 2)))))

