(ns celestial.integration.workflows
  "Testing workflows"
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.fixtures.data :refer 
     (redis-type redis-prox-spec redis-ec2-spec local-conf redis-ec2-centos)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.persistency.systems :as s]  
    [celestial.persistency :as p]  
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))


(defn spec 
  ([] (spec {}))
  ([m] (assoc (merge-with merge (s/get-system 1) m) :system-id 1)))

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
        (wf/destroy (spec)) => nil
        )
    ))

(with-admin
  (with-conf local-conf
    (with-state-changes [(before :facts (populate-system redis-type redis-ec2-spec))]
      (fact "aws creation workflows" :integration :ec2 :workflow
          (wf/create (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/stop (spec)) => nil 
          (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
          (wf/destroy (spec)) => nil)

      (fact "aws provisioning workflows" :integration :ec2 :workflow
          (wf/create (spec)) => nil
          (wf/reload (spec)) => nil 
          (wf/destroy (spec)) => nil)

      (fact "aws eip workflows" :integration :ec2 :workflow
        ; will be run only if EIP is defined
        (when-let [eip (System/getenv "EIP")]
          (wf/create (spec {:machine {:ip eip}})) => nil
          (wf/reload (spec {:machine {:ip eip}})) => nil 
          (wf/destroy (spec {:machine {:ip eip}})) => nil))

      (fact "aws with volumes" :integration :ec2 :workflow
        (let [with-vol {:aws {:volumes [{:device "/dev/sdn" :size 10 :clear true}]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil 
          (wf/destroy (spec with-vol)) => nil))

      (fact "aws puppetization" :integration :ec2 :workflow
          (wf/create (spec)) => nil
          (wf/puppetize redis-type (spec)) => nil 
          (wf/destroy (spec)) => nil)

      (fact "aws puppetization of s3" :integration :ec2 :workflow
          (let [s3-redis (assoc-in redis-type [:puppet-std :module :src] 
                            "s3://opsk-sandboxes/redis-sandbox-0.3.5.tar.gz")]
            (wf/create (spec)) => nil
            (wf/puppetize s3-redis (spec)) => nil 
            (wf/destroy (spec)) => nil))) 

(with-state-changes [(before :facts (populate-system redis-type redis-ec2-centos))]
     (fact "aws centos provisioning" :integration :ec2 :workflow
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil 
        (wf/puppetize redis-type (spec)) => (throws java.lang.AssertionError)
        (wf/start (spec)) => nil 
        (wf/puppetize redis-type (spec)) => nil
        (wf/destroy (spec)) => nil))))


