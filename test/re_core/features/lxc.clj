(ns re-core.features.lxc
  (:require
   [re-core.fixtures.data :refer (redis-type redis-lxc volume)]
   [re-core.integration.es.common :refer (populate-system)]
   [re-core.features.common :as common :refer (spec)]
   [re-core.workflows :as wf])
  (:use clojure.test))

(deftest lxc
  (testing "lxc creation workflows"
    (populate-system redis-type redis-lxc "1")
    (is (nil? (wf/create (spec))))
    (is (nil? (wf/stop (spec))))
    (is (nil? (wf/start (spec))))
    (is (nil? (wf/destroy (spec)))))

  #_(testing "lxc reload"
      (populate-system redis-type redis-lxc "1")
      (is (nil? (wf/create (spec))))
      (is (nil? (wf/reload (spec))))
      (is (nil? (wf/destroy (spec)))))

  #_(testing "lxc with volume"
      (populate-system redis-type redis-lxc "1")
      (let [with-vol {:lxc {:volumes [volume]}}]
        (is (nil? (wf/create (spec with-vol))))
        (is (nil? (wf/reload (spec with-vol))))
        (is (nil? (wf/destroy (spec with-vol)))))))

(use-fixtures :once common/setup)
