(ns celestial.test.vsphere
  (:require vsphere.provider)
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.fixtures :only (redis-vsphere-spec with-conf)]))

(fact "basic construction"
   (vconstruct redis-vsphere-spec)      
      )
