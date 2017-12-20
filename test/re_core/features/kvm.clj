(ns re-core.features.kvm
  (:require
   [re-core.log :refer (setup-logging)]
   [es.systems :as s]
   [re-core.model :refer (vconstruct)]
   [re-core.fixtures.core :refer (with-conf is-type?) :as f]
   [re-core.fixtures.data :refer (redis-type local-conf redis-gce)]
   [re-core.fixtures.populate :refer (populate-system)]
   [re-core.features.common :refer (spec)]
   [re-core.workflows :as wf]
   [re-core.fixtures.data :refer [redis-kvm]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(def volume {:device "vdb" :type "qcow2" :size 100 :clear true :pool :default :name "foo.img"})

(setup-logging)

(with-conf
  (fact "legal instance spec" :kvm
        (let [domain (vconstruct (assoc redis-kvm :system-id "1"))]
          (:system-id domain) => "1"
          (:node domain)  => (just {:user "ronen" :host "localhost" :port 22})
          (:domain domain) =>
          (just {:user "re-ops" :name "red1.local" :hostname "red1"
                 :image {:flavor :debian :template "ubuntu-16.04"}
                 :cpu 2 :ram 1024})))
  (fact "volume pool" :kvm
        (let [with-vol (assoc-in redis-kvm [:kvm :volumes] [volume])
              domain (vconstruct (assoc with-vol :system-id "1"))]
          (first (:volumes domain)) =>
          (just (assoc volume :pool {:id "default" :path "/var/lib/libvirt/images/"})))))

(with-conf local-conf
  (fact "kvm creation workflows" :integration :kvm :workflow
        (populate-system redis-type redis-kvm "1")
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil
        (wf/start (spec)) => nil
        (wf/destroy (spec)) => nil)

  (fact "kvm reload" :integration :kvm :workflow
        (populate-system redis-type redis-kvm "1")
        (wf/create (spec)) => nil
        (wf/reload (spec)) => nil
        (wf/destroy (spec)) => nil)

  (fact "kvm with volume" :integration :kvm :volume
        (populate-system redis-type redis-kvm "1")
        (let [with-vol {:kvm {:volumes [volume]}}]
          (wf/create (spec with-vol)) => nil
          (wf/reload (spec with-vol)) => nil
          (wf/destroy (spec with-vol)) => nil)))

