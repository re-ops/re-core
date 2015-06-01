(ns celestial.test.puppet
  (:require 
    [celestial.model :refer (pconstruct)]
    [celestial.fixtures.data :refer (redis-type redis-prox-spec)] 
    proxmox.provider aws.provider celestial.puppet_standalone); loading defmethods
  (:use midje.sweet))


(fact "puppet provision type construction"
  (pconstruct redis-type redis-prox-spec) =>
     (contains (get-in redis-prox-spec [:puppet-std :dev] )))


