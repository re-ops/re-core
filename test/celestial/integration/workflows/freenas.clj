(ns celestial.integration.workflows.freenas
  "openstack workflows"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (redis-type local-conf redis-freenas)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.workflows :as wf]
    )
  (:use midje.sweet)
 )

(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-freenas))]
      (fact "freenas creation workflows" :integration :freenas :workflow
          (wf/create (spec)) => nil 
          ;; (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          ;; (wf/stop (spec)) => nil 
          ;; (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          ;; (wf/destroy (spec)) => nil)
      ))))
