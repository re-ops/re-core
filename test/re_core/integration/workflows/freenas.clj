(ns re-core.integration.workflows.freenas
  "openstack workflows"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [re-core.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [re-core.persistency.systems :as s]
    [re-core.fixtures.data :refer (redis-type local-conf redis-freenas)]  
    [re-core.fixtures.populate :refer (populate-system)]  
    [re-core.integration.workflows.common :refer (spec)]
    [re-core.workflows :as wf])
  (:use midje.sweet))

(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-freenas))]
      (fact "freenas creation workflows" :integration :freenas :workflow
          (wf/create (spec)) => nil 
          ;; (wf/create (spec)) => (throws ExceptionInfo  (is-type? :re-core.workflows/machine-exists)) 
          (wf/stop (spec)) => nil 
          ;; (wf/create (spec)) => (throws ExceptionInfo  (is-type? :re-core.workflows/machine-exists)) 
          (wf/destroy (spec)) => nil
      ))))
