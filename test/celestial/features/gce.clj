(ns celestial.features.gce
  (:require 
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf) :as f]
    [celestial.fixtures.data :refer (redis-type local-conf redis-gce)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.workflows :as wf]
    [gce.provider :refer (build-compute)]
    [celestial.fixtures.data :refer [redis-gce]])
  (:use midje.sweet)
 )

(with-conf
  (let [machine-type "zones/europe-west1-d/machineTypes/n1-standard-1"
        source-image "projects/ubuntu-os-cloud/global/images/ubuntu-1404-trusty-v20151113" 
        {:keys [machine gce]} redis-gce]
    (fact "legal gce system" :gce
      (:gce (vconstruct redis-gce)) => 
         (contains {
           :name "red1"
           :machineType machine-type
           :disks [{
              :initializeParams {:sourceImage source-image} :autoDelete true
              :type "PERSISTENT" :boot true 
            }]
          })
      (provided (build-compute "") => nil))))


#_(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-freenas))]
      (fact "freenas creation workflows" :integration :gce :workflow
          (wf/create (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/stop (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/destroy (spec)) => nil
      ))))
