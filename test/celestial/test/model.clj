(ns celestial.test.model
  (:use 
    midje.sweet
    [celestial.model :only (translate)] 
    [celestial.common :only (slurp-edn)]
    ))

(def model (slurp-edn "fixtures/model.edn"))

(fact "constructing a proxmox model"
  (second (translate model)) => {:node "proxmox" :features ["nfs:on"]})
