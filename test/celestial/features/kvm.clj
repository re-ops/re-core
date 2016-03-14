(ns celestial.features.kvm
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf with-admin is-type?) :as f]
    [celestial.fixtures.data :refer (redis-type local-conf redis-gce)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.workflows :as wf]
    [celestial.fixtures.data :refer [redis-kvm]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo)
 )

(with-conf
  (let [{:keys [machine kvm]} redis-gce]
    (fact "legal instance spec" :kvm
      (let [domain (vconstruct (assoc redis-kvm :system-id 1))]
        (:system-id domain ) => 1
        (:node domain)  => (just {:user "ronen" :host "localhost" :port 22})
        (:domain domain) => 
          (just {
            :user "celestial" :name "red1.local" 
            :image {:flavor :debian :template "ubuntu-15.04"}
            :cpu 2 :ram 1024
           })))))

(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-kvm))]
      (fact "kvm creation workflows" :integration :kvm :workflow
           (wf/create (spec)) => nil 
           (wf/stop (spec)) => nil 
           (wf/start (spec)) => nil 
           (wf/destroy (spec)) => nil
          ))))
