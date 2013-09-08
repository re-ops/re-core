(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require 
    [celestial.persistency :as p] 
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
  (with-state-changes [(before :facts (clear-all))] 
    (fact "basic deploy" :integration :capistrano
      (p/add-type redis-type) 
      (let [id (p/add-system redis-prox-spec)
            cap (rconstruct redis-actions {:action :deploy :target "192.168.5.200"})]
         (reload redis-prox-spec)
         (.setup cap)
         (exists? (:dst cap)) => truthy 
         (.run cap)
         (.cleanup cap)
         (destroy (assoc redis-prox-spec :system-id id)) 
         (exists? (:dst cap))  => falsey 
         (p/system-exists? id)  => falsey))))

