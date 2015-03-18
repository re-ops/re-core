(ns celestial.integration.workflows.openstack
  "openstack workflows"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (redis-type local-conf redis-openstack)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.workflows :as wf]
    [openstack.networking :refer (addresses-ip)]
    [clojure.java.data :refer [from-java]]
    [openstack.common :refer (servers openstack block-storage)]
    [openstack.volumes :refer (status delete)])
  (:use midje.sweet)
 )

(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-openstack))]
      (fact "openstack creation workflows" :integration :openstack :workflow
          (wf/create (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/stop (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/destroy (spec)) => nil)

      (fact "openstack clone workflows" :integration :openstack :workflow
        (wf/create (spec)) => nil
        (wf/clone {:system-id 1 :hostname "bar" :owner "ronen"}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil)

      (fact "openstack provisioning workflows" :integration :openstack :workflow
          (wf/create (spec)) => nil
          (wf/reload (spec)) => nil 
          (wf/destroy (spec)) => nil)

      (fact "openstack puppetization" :integration :openstack :workflow
          (wf/create (spec)) => nil
          (wf/puppetize redis-type (spec)) => nil 
          (wf/destroy (spec)) => nil)

      (fact "openstack floating ip" :integration :openstack :workflow :floating
        ; will be run only if FIP env var is defined
        (when-let [fip (System/getenv "FIP")]
           (s/partial-system ((spec) :system-id) {:openstack {:floating-ip fip}})
           (:openstack (spec)) => (contains {:floating-ip fip})
           (wf/create (spec)) => nil
           (let [{:keys [tenant instance-id networks]} (:openstack (spec))]
             (second (addresses-ip (.get (servers tenant) instance-id) (first networks))) => fip)
           (wf/stop (spec)) => nil 
           (wf/reload (spec)) => nil 
           (:openstack (spec)) => (contains {:floating-ip fip})
           (wf/destroy (spec)) => nil))

       (fact "openstack with volumes" :integration :openstack :workflow
           (s/partial-system 
             (:system-id (spec)) {:openstack {:volumes [{:device "/dev/sdc" :size 20 :clear false}]}})     
           (wf/create (spec)) => nil
           (let [tenant (get-in (spec) [:openstack :tenant])
                 {:keys [id] :as v} (get-in (spec) [:openstack :volumes 0])]
              (status tenant id) => :in-use
              (wf/destroy (spec)) => nil
              (status tenant id) => :available
              (delete id tenant) => nil)
         ))))
