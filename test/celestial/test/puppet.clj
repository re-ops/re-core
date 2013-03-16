(ns celestial.test.puppet
  (:use 
    [celestial.model :only (pconstruct)]
    [celestial.fixtures :only (redis-type redis-prox-spec)] 
    expectations) 
  )


(expect {:type (assoc redis-type :hostname "red1")} (in (pconstruct redis-type redis-prox-spec)))
