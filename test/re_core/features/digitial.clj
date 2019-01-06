(ns re-core.features.digitial
  "digital ocean support"
  (:require
   [es.systems :as s]
   [rubber.node :refer (stop)]
   [re-core.fixtures.data :refer (redis-type)]
   [re-core.integration.es.common :refer (populate-system)]
   [re-core.features.common :as common :refer (spec get-spec)]
   [re-core.workflows :as wf]
   [re-core.fixtures.data :refer [redis-digital]])
  (:use clojure.test))

(defn add-system [f]
  (populate-system redis-type redis-digital "1")
  (f)
  (stop))

(deftest digitial
  (testing "digital-ocean creation workflows"
    (is (nil? (wf/create (spec))))
    (is (nil? (wf/stop (spec))))
    (is (nil? (wf/start (spec))))
    (is (nil? (wf/destroy (spec))))))

(use-fixtures :once add-system common/setup)
