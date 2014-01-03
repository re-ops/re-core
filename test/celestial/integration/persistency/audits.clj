(ns celestial.integration.persistency.audits
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.data :refer (basic-audit)]
    [celestial.redis :refer (clear-all)]  
    [celestial.persistency.audits :as a])
   (:use midje.sweet))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic audits usage" :integration :redis :audits
      (a/add-audit basic-audit)
      (a/get-audit "tid") =>  (assoc basic-audit :args ["tid"])))

