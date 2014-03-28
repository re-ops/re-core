(ns celestial.integration.persistency.audits
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-conf)]
    [celestial.fixtures.populate :refer (re-initlize)]
    [celestial.fixtures.data :refer (basic-audit)]
    [celestial.persistency.audits :as a])
   (:use midje.sweet))

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "basic audits usage" :integration :redis :audits
      (a/add-audit basic-audit)
      (a/get-audit "tid") =>  (assoc basic-audit :args ["tid"]))))

