(ns re-core.integration.workflows.ec2
  "ec2 workflows"
  (:require
    [aws.common :refer (with-ctx instance-desc)]
    [re-core.fixtures.core :refer (with-defaults is-type? with-conf)]
    [re-core.persistency.systems :as s]
    [re-core.fixtures.data :refer
     (redis-type redis-ec2-spec local-conf redis-ec2-centos)]
    [re-core.fixtures.populate :refer (populate-system)]
    [re-core.integration.workflows.common :refer (spec get-spec)]
    [re-core.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))

(defn add-vpc []
  (when-let [{:keys [subnet id]} (clojure.edn/read-string (System/getenv "VPC"))]
     (s/update-system 1
       (spec {
         :machine {:os :ubuntu-15.04}
         :aws {:vpc {:assign-public true :subnet-id subnet :vpc-id id}}}))))

(with-conf local-conf
  (with-state-changes [(before :facts (do (populate-system redis-type redis-ec2-spec) (add-vpc)))]
      (fact "aws creation workflows" :integration :ec2 :workflow
           (wf/create (spec)) => nil
           (wf/create (spec)) => (throws ExceptionInfo  (is-type? :re-core.workflows/machine-exists))
           (wf/stop (spec)) => nil
           (wf/create (spec)) => (throws ExceptionInfo  (is-type? :re-core.workflows/machine-exists))
           (wf/destroy (spec)) => nil)

      (fact "aws provisioning workflows" :integration :ec2 :workflow
          (wf/create (spec)) => nil
          (wf/reload (spec)) => nil
          (wf/destroy (spec)) => nil)

      (fact "vpc eip" :integration :ec2 :vpc
        (when-let [{:keys [eip id subnet]} (clojure.edn/read-string (System/getenv "VPC"))]
           (s/update-system 1 (spec {:machine {:ip eip :os :ubuntu-15.04}}))
           (wf/create (spec)) => nil
           (:machine (spec)) => (contains {:ip eip})
           (wf/stop (spec)) => nil
           (wf/reload (spec)) => nil
           (instance-desc (get-spec :aws :endpoint) (get-spec :aws :instance-id)) => (contains {:public-ip-address eip})
           (wf/destroy (spec)) => nil))

      (fact "aws with ebs volumes" :integration :ec2 :workflow
        (let [with-vol {:aws {:volumes [{:device "/dev/sdn" :size 10 :clear true :volume-type "standard" }]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil
          (wf/destroy (spec with-vol)) => nil))

      (fact "aws with ephemeral volumes" :integration :ec2 :workflow
        (let [with-vol {:aws {:instance-type "m3.medium" :block-devices [{:device-name "/dev/sdb" :virtual-name "ephemeral0" }]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil
          (wf/destroy (spec with-vol)) => nil))

      (fact "aws with zone and groups" :integration :ec2 :workflow :zone
        (let [with-zone {:aws {:availability-zone "eu-west-1a" :security-groups ["test"]}} ]
          (wf/create (spec with-zone)) => nil
          (let [{:keys [placement security-groups]}
                (instance-desc (get-spec :aws :endpoint) (get-spec :aws :instance-id))]
            placement => (contains {:availability-zone "eu-west-1a"})
            (first security-groups) => (contains {:group-name "Test"}))
          (wf/reload (spec with-zone)) => nil
          (wf/destroy (spec with-zone)) => nil))

      (fact "aws clone workflows" :integration :ec2 :workflow :clone
        (wf/create (spec)) => nil
        (wf/clone {:system-id 1 :hostname "bar" :owner "ronen"}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil)

      (fact "aws ebs-optimized" :integration :ec2 :workflow
        (wf/create (-> (spec)
          (assoc-in [:aws :instance-type] "m1.large")
          (assoc-in [:aws :ebs-optimized] true))) => nil
        (wf/destroy (spec)) => nil))

  (comment 
    "The following are deprecated or require special environment to run" 
    
    ; will be re-moted
    (fact "aws puppetization" :integration :ec2 :workflow :puppet :s3
          (wf/create (spec)) => nil
          (wf/provision redis-type (spec)) => nil
          (wf/destroy (spec)) => nil)

    ; will be re-moted
    (fact "aws puppetization of s3" :integration :ec2 :workflow :puppet
          (let [s3-redis (assoc-in redis-type [:puppet-std :dev :module :src] "s3://opsk-sandboxes/redis-sandbox-0.4.1.tar.gz")]
            (wf/create (spec)) => nil
            (wf/provision s3-redis (spec)) => nil
            (wf/destroy (spec)) => nil))

    ; require a vpn
    (fact "vpc private ip" :integration :ec2-vpn :vpc
        (when-let [{:keys [subnet id]} (clojure.edn/read-string (System/getenv "VPC"))]
          (s/update-system 1
            (spec {
              :machine {:os :ubuntu-15.04}
              :aws { :vpc {:assign-public false :subnet-id subnet :vpc-id id} }}))
          (wf/create (spec)) => nil
          (wf/stop (spec)) => nil
          (wf/reload (spec)) => nil
          (wf/destroy (spec)) => nil))
    (with-state-changes [(before :facts (populate-system redis-type redis-ec2-centos))]
     (fact "aws centos provisioning" :integration :ec2 :workflow
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil
        (wf/provision redis-type (spec)) => (throws java.lang.AssertionError)
        (wf/start (spec)) => nil
        (wf/provision redis-type (spec)) => nil
        (wf/destroy (spec)) => nil))
    )
  )
