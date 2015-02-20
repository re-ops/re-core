(ns celestial.integration.workflows.proxmox
  (:require 
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.fixtures.core :refer (with-defaults is-type?)]  
    [celestial.fixtures.data :refer 
     (redis-type redis-prox-spec local-conf)]
    [celestial.persistency.systems :as s]
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
        (wf/destroy (spec)) => nil)

    (fact "proxmox clone workflows" :integration :proxmox :workflow
        (wf/create (spec)) => nil
        (wf/clone  
          {:system-id 1 :hostname "bar" :owner "ronen" :machine {:ip "192.168.3.199"}}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil)
    ))




