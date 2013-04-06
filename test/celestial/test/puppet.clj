(ns celestial.test.puppet
  (:use 
    midje.sweet
    [celestial.model :only (pconstruct)]
    [celestial.fixtures :only (redis-type redis-prox-spec)] 
    )
  )


(fact "puppet provision type construction"
   (pconstruct redis-type redis-prox-spec) =>  (contains {:type (assoc redis-type :hostname "red1")}))
