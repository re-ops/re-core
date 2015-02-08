(ns celestial.integration.workflows.ec2
  "ec2 workflows"
  (:require 
    [aws.common :refer (with-ctx instance-desc)]
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer 
     (redis-type redis-ec2-spec local-conf redis-ec2-centos)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))

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

      (fact "aws with ebs volumes" :integration :ec2 :workflow
        (let [with-vol {:aws {:volumes [{:device "/dev/sdn" :size 10 :clear true :volume-type "standard" }]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil 
          (wf/destroy (spec with-vol)) => nil))

      (fact "aws with ephemeral volumes" :integration :ec2 :workflow
        (let [with-vol {:aws {:instance-type "m1.small" :block-devices [{:device-name "/dev/sdb" :virtual-name "ephemeral0" }]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil 
          (wf/destroy (spec with-vol)) => nil))

      (fact "aws with zone and groups" :integration :ec2 :workflow
        (let [with-zone {:aws {:availability-zone "eu-west-1a" :security-groups ["test"]}} ]
          (wf/create (spec with-zone)) => nil
          (let [{:keys [placement security-groups]} 
                (instance-desc (get-spec :aws :endpoint) 
                               (get-spec :aws :instance-id))]
            placement  => (contains {:availability-zone "eu-west-1a"})
            (first security-groups) => (contains {:group-name "Test"}))
          (wf/reload (spec with-zone)) => nil 
          (wf/destroy (spec with-zone)) => nil))

      (fact "aws puppetization" :integration :ec2 :workflow
          (wf/create (spec)) => nil
          (wf/puppetize redis-type (spec)) => nil 
          (wf/destroy (spec)) => nil)

      (fact "aws puppetization of s3" :integration :ec2 :workflow
          (let [s3-redis (assoc-in redis-type [:puppet-std :module :src] 
                            "s3://opsk-sandboxes/redis-sandbox-0.3.5.tar.gz")]
            (wf/create (spec)) => nil
            (wf/puppetize s3-redis (spec)) => nil 
            (wf/destroy (spec)) => nil))
      
      (fact "aws clone workflows" :integration :ec2 :workflow
        (wf/create (spec)) => nil
        (wf/clone 1 {:hostname "bar" :owner "ronen"}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil)

      (fact "aws ebs-optimized" :integration :ec2 :workflow
        (wf/create (-> (spec) 
          (assoc-in [:aws :instance-type] "m1.large")            
          (assoc-in [:aws :ebs-optimized] true))) => nil
        (wf/destroy (spec)) => nil)
      ) 

  (with-state-changes [(before :facts (populate-system redis-type redis-ec2-centos))]
     (fact "aws centos provisioning" :integration :ec2 :workflow
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil 
        (wf/puppetize redis-type (spec)) => (throws java.lang.AssertionError)
        (wf/start (spec)) => nil 
        (wf/puppetize redis-type (spec)) => nil
        (wf/destroy (spec)) => nil))))
