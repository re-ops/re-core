(ns celestial.integration.workflows
  "Testing workflows"
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.fixtures.data :refer (redis-type redis-prox-spec redis-ec2-spec local-conf redis-ec2-centos)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.persistency.systems :as s]  
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))


(with-defaults 
  (with-state-changes [(before :facts (populate-system redis-type redis-prox-spec))]
    (fact "proxmox creation workflows" :integration :proxmox :workflow
      (letfn [(spec [] (assoc (s/get-system 1) :system-id 1))] 
        (wf/create (spec)) => nil 
        (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/stop (spec)) => nil 
        (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/destroy (spec)) => nil))

    (fact "proxmox reload workflows" :integration :ec2 :workflow
      (letfn [(spec [] (assoc (s/get-system 1) :system-id 1))] 
        (wf/create (spec)) => nil
        (wf/reload (spec)) => nil 
        (wf/destroy (spec)) => nil
        ))
    ))

(with-admin
 (with-conf local-conf
  (with-state-changes [(before :facts (do (populate-system redis-type redis-ec2-spec)))]
    (fact "aws creation workflows" :integration :ec2 :workflow
      (letfn [(spec [] (assoc (s/get-system 1) :system-id 1))] 
        (wf/create (spec)) => nil 
        (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/stop (spec)) => nil 
        (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/destroy (spec)) => nil
        ))

    (fact "aws provisioning workflows" :integration :ec2 :workflow
      (letfn [(spec [] (assoc (s/get-system 1) :system-id 1))] 
        (wf/create (spec)) => nil
        (wf/reload (spec)) => nil 
        (wf/destroy (spec)) => nil
        ))) 

    (with-state-changes [(before :facts (do (populate-system redis-type redis-ec2-centos)))]
     (fact "aws centos provisioning" :integration :ec2 :workflow
      (letfn [(spec [] (assoc (s/get-system 1) :system-id 1))] 
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil 
        (wf/puppetize redis-type (spec)) => (throws java.lang.AssertionError)
        (wf/start (spec)) => nil 
        (wf/puppetize redis-type (spec)) => nil
        (wf/destroy (spec)) => nil
        )))
    ))


