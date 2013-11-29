(ns celestial.integration.workflows.proxmox
  (:require 
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.fixtures.core :refer (with-defaults is-type?)]  
    [celestial.fixtures.data :refer 
     (redis-type redis-prox-spec local-conf)]
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))

(with-defaults 
  (with-state-changes [(before :facts (populate-system redis-type redis-prox-spec))]
    (fact "proxmox creation workflows" :integration :proxmox :workflow
      (wf/create (spec)) => nil 
      (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
      (wf/stop (spec)) => nil 
      (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
      (wf/destroy (spec)) => nil)

    (fact "proxmox reload workflows" :integration :proxmox :workflow
        (wf/create (spec)) => nil
        (wf/reload (spec)) => nil 
        (wf/destroy (spec)) => nil)))




