(ns celestial.persistchecks.stacks
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
    (fact "basic stack persistency" :persistency :redis :stacks
      (s/add-stack simple-stack) => 1
      (s/update-stack "1" (assoc-in simple-stack [:shared :env] :qa)) 
      (get-in (s/get-stack 1) [:shared :env])  => :qa
      (s/delete-stack "1") => 1
      )
   ))
