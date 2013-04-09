(ns celestial.integration.capistrano
  "Capistrano remoter provider"
  (:use 
    midje.sweet 
    [celestial.launch :only (setup-logging)]
    [celestial.fixtures :only (cap-deploy)]
    [celestial.model :only (rconstruct)]
    )  
  (:import [capistrano.remoter Capistrano])
  )

(fact "basic deploy" :integration :sshj
   (let [cap (rconstruct cap-deploy)]
     (setup-logging)
     (.run cap "")))

