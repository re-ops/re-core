(ns celestial.integration.gc.openstack
(:require 
    [celestial.security :refer (set-user current-user)]
    [openstack.gc :refer (find-candidates)]
    [celestial.persistency.systems :as s]
    [celestial.fixtures.core :refer (is-type? with-defaults with-conf)]
    [celestial.fixtures.populate :refer (populate-all)])
  (:use midje.sweet)
 )

(def machines [ 
  {:machine {} :openstack {:instance-id "1"}} 
  {:machine {} :openstack {:instance-id "4"}}])


(fact "find openstack candidates" :gc :openstack 
   (find-candidates machines ["1" "2" "3"]) => (contains "4")
   (find-candidates machines []) => (contains "1" "4"))
