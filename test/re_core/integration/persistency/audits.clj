(ns re-core.integration.persistency.audits
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [re-core.fixtures.core :refer (with-conf)]
    [re-core.fixtures.populate :refer (re-initlize)]
    [re-core.fixtures.data :refer (basic-audit)]
    [re-core.persistency.audits :as a])
   (:use midje.sweet))

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "basic audits usage" :integration :redis :audits
      (a/add-audit basic-audit)
      (a/get-audit "tid") =>  (assoc basic-audit :args ["tid"]))))

