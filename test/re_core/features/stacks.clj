(ns re-core.features.stacks
  "Stack feature"
  (:require
   [flatland.useful.map :refer  (dissoc-in*)]
   [re-core.persistency.stacks :as s]
   [re-core.fixtures.data :refer (simple-stack)]
   [re-core.fixtures.core :refer (with-conf)]
   [re-core.fixtures.core :refer (with-m?)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "stack persistency" :redis :stacks :persistency :integration
          (s/add-stack simple-stack) => 1
          (s/update-stack "1" (assoc-in simple-stack [:shared :env] :qa))
          (get-in (s/get-stack 1) [:shared :env])  => :qa
          (s/delete-stack "1") => 1)))

(fact "stack validations" :validations :stacks
      (s/validate-stack (dissoc-in* simple-stack [:shared :owner])) =>
      (throws ExceptionInfo (with-m? {:shared {:owner "must be present"}}))
      (s/validate-stack (assoc-in simple-stack [:systems 2] {})) =>
      (throws ExceptionInfo
              (with-m? '{:systems ({2 {:count "must be present", :template "must be present"}})})))
