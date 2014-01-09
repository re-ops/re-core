(ns celestial.test.model
  "General model flow facts"
  (:require 
    proxmox.provider proxmox.model
    [celestial.model :refer (translate clone)] 
    [celestial.common :refer (slurp-edn)]
    [celestial.fixtures.core :refer (with-conf)])
  (:use midje.sweet))

(def model (assoc (slurp-edn "fixtures/model.edn") :system-id 1))

(fact "constructing a proxmox model"
  (with-conf 
    (second (translate model))) => 
      {:node "proxmox" :flavor :debian :features ["nfs:on"] :system-id 1})

(fact "cloning purge"
   (:proxmox (clone model)) =not=> (contains [[:vmid 101]])
   (:machine (clone model)) =not=> (contains [[:ip "192.168.5.33"]]))
