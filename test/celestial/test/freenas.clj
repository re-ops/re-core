(ns celestial.test.freenas
  (:require 
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.data :refer [redis-freenas]])
  (:use midje.sweet))

#_(with-conf
  (let [{:keys [machine freenas]} redis-freenas]
    (fact "legal freenas system"
       (vconstruct redis-freenas) => nil
      )
    
    ))
