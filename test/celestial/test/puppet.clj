(ns celestial.test.puppet
  (:use 
    [celestial.model :only (pconstruct)]
    [celestial.fixtures :only (redis-type spec)] 
    expectations) 
  )


(expect {:type redis-type} (in (pconstruct redis-type spec)))
