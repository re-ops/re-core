(ns re-core.features.digitial
  "digital ocean support"
  (:require
   [es.systems :as s]
   [rubber.node :refer (stop)]
   [re-core.fixtures.data :refer (redis-type)]
   [re-core.fixtures.populate :refer (populate-system)]
   [re-core.fixtures.core :refer (with-dev)]
   [re-core.features.common :refer (spec get-spec)]
   [re-core.workflows :as wf]
   [re-core.model :refer (vconstruct)]
   [re-core.fixtures.data :refer [redis-digital]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(with-dev
  (let [{:keys [machine digital-ocean]} redis-digital]
    (fact "legal digital-ocean system" :digital-ocean
          (:drp (vconstruct redis-digital)) => (contains {:name "red1.local"}))))

(with-state-changes [(before :facts (populate-system redis-type redis-digital "1")) (after :facts (stop))]
  (fact "digital-ocean creation workflows" :integration :digital-ocean :workflow
        (wf/create (spec)) => nil
        (wf/stop (spec)) => nil
        (wf/start (spec)) => nil
        (wf/destroy (spec)) => nil))
