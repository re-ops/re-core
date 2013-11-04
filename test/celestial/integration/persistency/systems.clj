(ns celestial.integration.persistency.systems
  "systems persistency tests"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.security :refer (set-user current-user)]
    [celestial.persistency.systems :as s]
    [celestial.fixtures.core :refer (is-type? with-defaults with-conf)]
    [celestial.fixtures.populate :refer (populate-all)])
  (:use midje.sweet)) 

(with-conf
  (with-state-changes [(before :facts (set-user {:username "admin"} (populate-all)))]
   (fact "systems for user" :integration :redis :systems
     #_(set-user {:username "ronen"} 
       (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "ronen")) 
          => (has every? (partial = :dev))) 
     (set-user {:username "admin"} 
       (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "ronen")) 
          => (has every? (partial = :dev)))
     (set-user {:username "ronen"} 
        (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "admin")) 
          => (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-violation)))
     (set-user {:username "admin"} 
        (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "admin")) 
          => (has every? #{:dev :qa :prod}))
     )))

