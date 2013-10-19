(ns celestial.integration.workflows
  "Testing workflows"
  (:require 
    [celestial.fixtures.core :refer  (with-defaults is-type? with-admin with-conf)]  
    [celestial.fixtures.data :refer (redis-type redis-prox-spec redis-ec2-spec local-conf)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.persistency.systems :as s]  
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))


(with-defaults 
  (with-state-changes [(before :facts (populate-system redis-type redis-prox-spec))]
    (fact "proxmox creation workflows" :integration :proxmox :workflow
      (let [spec (assoc (s/get-system 1) :system-id 1)] 
        (wf/create spec) => nil 
        (wf/create spec) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/stop spec) => nil 
        (wf/create spec) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/destroy spec) => nil))))

(with-admin
 (with-conf local-conf
  (with-state-changes [(before :facts (populate-system redis-type redis-ec2-spec))]
    (fact "aws creation workflows" :integration :ec2 :workflow
      (let [spec (assoc (s/get-system 1) :system-id 1)] 
        (wf/create spec) => nil 
        (wf/create spec) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/stop spec) => nil 
        (wf/create spec) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
        (wf/destroy spec) => nil)))))
