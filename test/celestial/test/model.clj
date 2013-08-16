(ns celestial.test.model
  "General model flow facts"
  (:require proxmox.provider proxmox.model)
  (:use 
    midje.sweet
    [celestial.model :only (translate clone)] 
    [celestial.common :only (slurp-edn)]
    [celestial.fixtures :only (with-conf)] 
    ))

(def model (assoc (slurp-edn "fixtures/model.edn") :system-id 1))

(fact "constructing a proxmox model"
  (with-conf 
    (second (translate model))) => {:node "proxmox" :flavor :debian :features ["nfs:on"] :system-id 1})

(fact "cloning purge"
   (:proxmox (clone model)) =not=> (contains [[:vmid 101]])
   (:machine (clone model)) =not=> (contains [[:ip "192.168.5.33"]]))
