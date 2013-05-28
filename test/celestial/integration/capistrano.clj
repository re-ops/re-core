(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require capistrano.remoter)
  (:use 
    midje.sweet 
    [celestial.fixtures :only (redis-actions redis-prox-spec)]
    [me.raynes.fs :only (exists?)]
    [celestial.workflows :only (reload destroy)]
    [celestial.model :only (rconstruct)])  
  )

(fact "basic deploy" :integration :capistrano
   (let [cap (rconstruct redis-actions {:action :deploy :target "192.168.5.100"})]
     (reload redis-prox-spec)
     (.setup cap)
     (exists? (:dst cap)) => truthy 
     (.run cap)
     (.cleanup cap)
     (destroy redis-prox-spec) 
     (exists? (:dst cap))  => falsey 
     ))

