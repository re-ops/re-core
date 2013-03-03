(ns celestial.test.model
  (:use 
    [celestial.model :only (translate)] 
    [celestial.common :only (slurp-edn)]
     expectations))

(def model (slurp-edn "fixtures/model.edn"))

(expect {:node "proxmox" :features ["nfs:on"]}  (second (translate model)))
