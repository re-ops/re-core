(ns re-core.features.kvm
  (:require
   [es.systems :as s]
   [re-core.fixtures.data :refer (redis-type redis-kvm volume)]
   [re-core.integration.es.common :refer (populate-system)]
   [re-core.features.common :as common :refer (spec)]
   [re-core.workflows :as wf])
  (:use clojure.test))

(deftest kvm
  (testing "kvm creation workflows"
    (populate-system redis-type redis-kvm "1")
    (is (nil? (wf/create (spec))))
    (is (nil? (wf/stop (spec))))
    (is (nil? (wf/start (spec))))
    (is (nil? (wf/destroy (spec)))))

  #_(testing "kvm reload"
      (populate-system redis-type redis-kvm "1")
      (is (nil? (wf/create (spec))))
      (is (nil? (wf/reload (spec))))
      (is (nil? (wf/destroy (spec)))))

  #_(testing "kvm with volume"
      (populate-system redis-type redis-kvm "1")
      (let [with-vol {:kvm {:volumes [volume]}}]
        (is (nil? (wf/create (spec with-vol))))
        (is (nil? (wf/reload (spec with-vol))))
        (is (nil? (wf/destroy (spec with-vol)))))))

(use-fixtures :once common/setup)
