(ns re-core.integration.persistency.systems
  "systems persistency tests"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [es.systems :as es :refer (set-flush)]
    [re-core.security :refer (set-user current-user)]
    [re-core.persistency.systems :as s]
    [re-core.fixtures.core :refer (is-type? with-defaults with-conf)]
    [re-core.fixtures.populate :refer (populate-all)])
  (:use midje.sweet)) 

(set-flush true
 (with-conf
   (with-state-changes [(before :facts (set-user {:username "admin"} (populate-all)))]
      (fact "systems for user" :integration :redis :systems :elasticsearch
        (set-user {:username "admin"} ; admin is set to have access to all envs
          (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "ronen")) 
             => (contains [:qa :dev :prod] :gaps-ok :in-any-order))
        (set-user {:username "ronen"} 
           (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "admin")) 
             => (throws ExceptionInfo (is-type? :re-core.persistency.systems/persmission-owner-violation)))
        (set-user {:username "admin"} 
           (mapv #(-> (Integer/valueOf %) s/get-system :env) (s/systems-for "admin")) 
             => (has every? #{:dev :qa :prod})))
      (fact "re-indexing" :integration :redis :systems :elasticsearch
        (get-in (es/systems-for "admin" {} 0 10) [:hits :total]) => 100
        (set-user {:username "admin"} (s/re-index "admin"))
        (get-in (es/systems-for "admin" {} 0 10) [:hits :total] ) => 100))))

