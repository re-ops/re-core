(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require 
    [celestial.persistency :as p] 
    capistrano.remoter)
  (:use 
    midje.sweet 
    [celestial.fixtures :only (redis-actions redis-prox-spec redis-type)]
    [me.raynes.fs :only (exists?)]
    [celestial.workflows :only (reload destroy)]
    [celestial.model :only (rconstruct)])  
  )

(fact "basic deploy" :integration :capistrano
   (p/add-type redis-type) 
   (let [id (p/add-system redis-prox-spec)
         cap (rconstruct redis-actions {:action :deploy :target "192.168.5.100"})]
     (reload redis-prox-spec)
     (.setup cap)
     (exists? (:dst cap)) => truthy 
     (.run cap)
     (.cleanup cap)
     (destroy id redis-prox-spec) 
     (exists? (:dst cap))  => falsey 
     (p/system-exists? id)  => falsey 
     ))

