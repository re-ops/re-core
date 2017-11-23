(ns re-core.features.kvm
  (:require
   [re-core.log :refer (setup-logging debug-on)]
   [re-core.persistency.systems :as s]
   [re-core.model :refer (vconstruct)]
   [re-core.fixtures.core :refer (with-conf is-type?) :as f]
   [re-core.fixtures.data :refer (redis-type local-conf redis-gce)]
   [re-core.fixtures.populate :refer (populate-system)]
   [re-core.integration.workflows.common :refer (spec)]
   [re-core.workflows :as wf]
   [re-core.fixtures.data :refer [redis-kvm]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(setup-logging)

(with-conf
  (let [{:keys [machine kvm]} redis-gce]
    (fact "legal instance spec" :kvm
          (let [domain (vconstruct (assoc redis-kvm :system-id 1))]
            (:system-id domain) => 1
            (:node domain)  => (just {:user "ronen" :host "localhost" :port 22})
            (:domain domain) =>
            (just {:user "re-ops" :name "red1.local" :hostname "red1"
                   :image {:flavor :debian :template "ubuntu-16.04"}
                   :cpu 2 :ram 1024})))))

(with-conf local-conf
  (with-state-changes [(before :facts (populate-system redis-type redis-kvm))]
    (fact "kvm creation workflows" :integration :kvm :workflow
          (wf/create (spec)) => nil
          (wf/stop (spec)) => nil
          (wf/start (spec)) => nil
          (wf/destroy (spec)) => nil)

    (fact "kvm reload" :integration :kvm :workflow
          (wf/create (spec)) => nil
          (wf/reload (spec)) => nil
          (wf/destroy (spec)) => nil)

    (fact "kvm with volume" :integration :kvm :kvolume
          (let [with-vol {:kvm {:volumes [{:device "vdb" :type "qcow2" :size 100 :clear true}]
}}]
            (wf/create (spec with-vol)) => nil
            (wf/reload (spec)) => nil
            (wf/destroy (spec with-vol)) => nil))))

(comment
  (with-conf local-conf (wf/destroy (s/get-system 1))))
