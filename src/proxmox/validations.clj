(ns proxmox.validations
  "Proxmox provider based validations"
  (:use 
    [bouncer [core :as b] [validators :as v :only (defvalidatorset )]])
  (:require 
    [celestial.validations :as cv]
    ))



(defvalidatorset machine-common
    :cpus [v/number v/required]
    :disk [v/number v/required]
    :memory [v/number v/required]
    :hostname [v/required cv/str?])

(defvalidatorset proxmox-entity
    :password [v/required cv/str?]
    :nameserver [cv/str?])

(defvalidatorset machine-entity
     :ip [cv/str?]
     :os [v/required cv/keyword?] )

(defvalidatorset entity-validation
   :machine machine-common 
   :machine machine-entity
   :proxmox proxmox-entity
  )

(defn validate-entity
 "proxmox based system entity validation for persistence layer" 
  [proxmox]
   (cv/validate!! ::invalid-system proxmox entity-validation))

(defvalidatorset extended
    :id [v/number]          
    :features [cv/sequential?])

(defvalidatorset machine-provider
     :vmid [v/required v/number]
     :password [v/required cv/str?]
     :nameserver [cv/str?] 
     :ip_address [cv/str?]
     :ostemplate [v/required cv/str?] )

(defvalidatorset provider-validation
   :machine machine-common 
   :machine machine-provider
   :extended extended
  )

(defn provider-validation
  "Almost the same validation as persisted with small mapped properties modifications"
  [machine extended]
    (cv/validate!! ::invalid-container {:machine machine :extended extended} provider-validation))

