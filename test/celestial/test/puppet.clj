(ns celestial.test.puppet
  (:require proxmox.provider aws.provider celestial.puppet_standalone); loading defmethods
  (:use 
    midje.sweet
    [celestial.model :only (pconstruct)]
    [celestial.fixtures :only (redis-type redis-prox-spec)] 
    )
  )


(fact "puppet provision type construction"
   (pconstruct redis-type redis-prox-spec) =>  (contains {:type (assoc redis-type :hostname "red1.local")}))
