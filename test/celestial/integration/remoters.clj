(ns celestial.integration.remoters
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require 
    [celestial.persistency :as p] 
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (redis-deploy redis-runall redis-prox-spec redis-type)]
    [celestial.fixtures.populate :refer (add-users)]
    [celestial.fixtures.core :refer (with-defaults)]
    [celestial.redis :refer (clear-all)]
    [me.raynes.fs :refer (exists?)]
    [celestial.workflows :refer (reload destroy)]
    [celestial.model :refer (rconstruct)]
    remote.capistrano)
  (:use midje.sweet))

(with-defaults
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type) (add-users)))] 
    (fact "basic deploy" :integration :capistrano
      (let [id (s/add-system redis-prox-spec) 
            spec (assoc redis-prox-spec :system-id id)
            cap (rconstruct redis-deploy {:target "192.168.3.200"})]
         (reload spec)
         (.setup cap)
         (exists? (:dst cap)) => truthy 
         (.run cap)
         (.cleanup cap)
         (destroy spec) 
         (exists? (:dst cap))  => falsey 
         (s/system-exists? id)  => falsey)))
  
    (fact "ruby runall" :integration :ruby :capistrano
      (let [id (s/add-system redis-prox-spec)
            spec (assoc redis-prox-spec :system-id id)
            cap (rconstruct redis-runall {:target "192.168.3.200"}) ]
         (reload spec)
         (.setup cap)
         (exists? (:dst cap)) => truthy 
         (.run cap)
         (.cleanup cap)
         (destroy spec)
         (exists? (:dst cap))  => falsey 
         (s/system-exists? id)  => falsey))) 
