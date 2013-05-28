(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require capistrano.remoter)
  (:use 
    midje.sweet 
    [celestial.fixtures :only (redis-actions redis-prox-spec)]
    [celestial.workflows :only (reload destroy)]
    [celestial.model :only (rconstruct)])  
  )

(fact "basic deploy" :integration :capistrano
   (let [cap (rconstruct redis-actions {:action :deploy :target "192.168.5.33"}) 
         ]
     (reload redis-prox-spec)
     (.setup cap)
     (.run cap)
     (.cleanup cap)
     (destroy redis-prox-spec) 
     ))

