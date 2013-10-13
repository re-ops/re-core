(ns celestial.integration.remoters
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require 
    [celestial.persistency :as p] 
    [celestial.persistency.systems :as s]
    remote.capistrano)
  (:use 
    midje.sweet 
    [celestial.redis :only (clear-all)]
    [celestial.fixtures :only (redis-actions redis-prox-spec redis-type with-conf)]
    [me.raynes.fs :only (exists?)]
    [celestial.workflows :only (reload destroy)]
    [celestial.model :only (rconstruct)])  
  )

(with-conf
  (with-state-changes [(before :facts ((clear-all) (p/add-type redis-type)) )] 
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
