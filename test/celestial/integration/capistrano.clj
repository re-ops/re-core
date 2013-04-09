(ns celestial.integration.capistrano
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:use 
    midje.sweet 
    [celestial.launch :only (setup-logging)]
    [celestial.fixtures :only (cap-deploy)]
    [celestial.model :only (rconstruct)]
    )  
  (:require capistrano.remoter)
  (:import [capistrano.remoter Capistrano])
  )

#_(fact "basic deploy" :integration :sshj
   (let [cap (rconstruct cap-deploy)]
     (setup-logging)
     (.run cap "")))

