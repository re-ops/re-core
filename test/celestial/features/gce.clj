(ns celestial.features.gce
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf with-admin is-type?) :as f]
    [celestial.fixtures.data :refer (redis-type local-conf redis-gce)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.workflows :as wf]
    [gce.provider :refer (build-compute)]
    [celestial.fixtures.data :refer [redis-gce]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo)
 )

(with-conf
  (let [machine-type "zones/europe-west1-d/machineTypes/n1-standard-1"
        source-image "projects/ronen-playground/global/images/ubuntu-1510-puppet-382-1451476982" 
        {:keys [machine gce]} redis-gce]

    (fact "legal instance spec" :gce
      (:spec (vconstruct (assoc redis-gce :system-id 1))) => 
         (contains {:system-id 1 })
      (provided (build-compute "/home/ronen/compute-playground.json") => nil))

    (fact "legal instance gce" :gce
      (:gce (vconstruct redis-gce)) => 
         (contains {
           :name "red1"
           :machineType machine-type
           :disks [{
              :initializeParams {:sourceImage source-image} :autoDelete true
              :type "PERSISTENT" :boot true 
            }]
          })

     (provided (build-compute "/home/ronen/compute-playground.json") => nil))))


(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-gce))]
      (fact "gce creation" :integration :gce :workflow
          (wf/create (spec)) => nil 
          (wf/create (spec)) => 
             (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/stop (spec)) => nil 
          (wf/create (spec)) => 
            (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/destroy (spec)) => nil)

     (fact "gce clone" :integration :gce :workflow
        (wf/create (spec)) => nil
        (wf/clone {:system-id 1 :hostname "bar" :owner "ronen"}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil)

     (fact "gce provisioning" :integration :gce :workflow
        (wf/create (spec)) => nil
        (wf/reload (spec)) => nil 
        (wf/destroy (spec)) => nil)

     (fact "gce puppetization" :integration :gce :workflow :puppet
        (wf/create (spec)) => nil
        (wf/provision redis-type (spec)) => nil 
        (wf/destroy (spec)) => nil))))
