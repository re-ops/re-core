(ns re-core.features.kvm
  (:require
   [re-core.log :refer (setup-logging)]
   [es.systems :as s]
   [re-core.fixtures.core :as f :refer (with-dev)]
   [re-core.fixtures.data :refer (redis-type local-conf)]
   [re-core.fixtures.populate :refer (populate-system)]
   [re-core.features.common :refer (spec)]
   [re-core.workflows :as wf]
   [re-core.fixtures.data :refer [redis-kvm]])
  (:use clojure.test)
  (:import clojure.lang.ExceptionInfo))

(def volume {:device "vdb" :type "qcow2" :size 100 :clear true :pool :default :name "foo.img"})

(setup-logging)

(deftest kvm
  (with-dev
    (testing "kvm creation workflows"
      (populate-system redis-type redis-kvm "1")
      (is (nil? (wf/create (spec))))
      (is (nil? (wf/stop (spec))))
      (is (nil? (wf/start (spec))))
      (is (nil? (wf/destroy (spec)))))

    (testing "kvm reload"
      (populate-system redis-type redis-kvm "1")
      (is (nil? (wf/create (spec))))
      (is (nil? (wf/reload (spec))))
      (is (nil? (wf/destroy (spec)))))

    (testing "kvm with volume"
      (populate-system redis-type redis-kvm "1")
      (let [with-vol {:kvm {:volumes [volume]}}]
        (is (nil? (wf/create (spec with-vol))))
        (is (nil? (wf/reload (spec with-vol))))
        (is (nil? (wf/destroy (spec with-vol))))))))
