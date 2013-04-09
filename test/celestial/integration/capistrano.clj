(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require capistrano.remoter)
  (:use 
    midje.sweet 
    [celestial.fixtures :only (cap-deploy)]
    [celestial.model :only (rconstruct)])  
  )

(fact "basic deploy" :integration :sshj
   (let [cap (rconstruct cap-deploy)]
     (.setup cap)
     (.run cap "")
     (.cleanup cap)))

