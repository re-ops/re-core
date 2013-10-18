(ns celestial.integration.remoters
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require 
    [celestial.persistency :as p] 
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (redis-actions redis-prox-spec redis-type)]
    [celestial.fixtures.core :refer (with-defaults)]
    [celestial.redis :refer (clear-all)]
    [me.raynes.fs :refer (exists?)]
    [celestial.workflows :refer (reload destroy)]
    [celestial.model :refer (rconstruct)]   
     remote.capistrano)
  (:use midje.sweet)
  )

(with-defaults
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type)) )] 
    (fact "basic deploy" :integration :capistrano
      (let [id (s/add-system redis-prox-spec)
            cap (rconstruct redis-actions {:action :deploy :target "192.168.5.200"})]
         (reload redis-prox-spec)
         (.setup cap)
         (exists? (:dst cap)) => truthy 
         (.run cap)
         (.cleanup cap)
         (destroy (assoc redis-prox-spec :system-id id)) 
         (exists? (:dst cap))  => falsey 
         (s/system-exists? id)  => falsey)))
  
    (fact "ruby run-all" :integration :ruby
      (let [id (s/add-system redis-prox-spec)
            cap (rconstruct redis-actions {:action :run-all :target "192.168.5.200"})]
         (reload redis-prox-spec)
         (.setup cap)
         (exists? (:dst cap)) => truthy 
         (.run cap)
         (.cleanup cap)
         (destroy (assoc redis-prox-spec :system-id id)) 
         (exists? (:dst cap))  => falsey 
         (s/system-exists? id)  => falsey))) 
