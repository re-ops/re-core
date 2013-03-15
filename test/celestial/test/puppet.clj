(ns celestial.test.puppet
  (:use 
    [celestial.model :only (pconstruct)]
    [celestial.fixtures :only (redis-type spec)] 
    expectations) 
  )


(expect {:type (assoc redis-type :hostname "red1")} (in (pconstruct redis-type spec)))
