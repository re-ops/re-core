(ns celestial.integration.workflows.openstack
  "openstack workflows"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer 
     (redis-type local-conf redis-openstack)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.workflows :as wf])
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

      #_(fact "openstack floating ip":integration :openstack :workflow
        ; will be run only if EIP env var is defined
        (when-let [eip (System/getenv "EIP")]
           (wf/create (spec {:machine {:ip eip}})) => nil
           (:machine (spec)) => (contains {:ip eip})
           (wf/stop (spec)) => nil 
           (wf/reload (spec)) => nil 
           (instance-desc (get-spec :aws :endpoint) (get-spec :aws :instance-id))
             => (contains {:public-ip-address eip}) 
           (:machine (spec)) => (contains {:ip eip})
           (wf/destroy (spec)) => nil
          ))

      
      
      )))
