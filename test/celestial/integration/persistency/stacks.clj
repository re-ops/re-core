(ns celestial.integration.persistency.stacks
  "Testing basic stacks persisetency"
  (:require 
    [celestial.persistency.stacks :as s]
    [celestial.fixtures.data :refer (simple-stack)]
    [celestial.fixtures.core :refer (with-conf)]
    [celestial.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet)
 )

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
   (fact "basic stack usage" :integration :redis :stacks
     (s/add-stack simple-stack) => nil
     )
   ))
