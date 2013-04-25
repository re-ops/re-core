(ns celestial.test.model
  (:require proxmox.provider)
  (:use 
    midje.sweet
    [celestial.model :only (translate)] 
    [celestial.common :only (slurp-edn)]
    [celestial.fixtures :only (with-conf)] 
    ))

(def model (assoc (slurp-edn "fixtures/model.edn") :system-id 1))

(fact "constructing a proxmox model"
  (with-conf 
    (second (translate model))) => {:node "proxmox" :features ["nfs:on"] :system-id 1})
