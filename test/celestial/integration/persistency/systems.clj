(ns celestial.integration.persistency.systems
  "systems persistency tests"
  (:use 
    midje.sweet 
    [celestial.roles :only (admin)]
    [celestial.persistency :only (validate-type validate-quota user-exists? validate-user)]
    [celestial.fixtures :only (redis-type is-type? user-quota with-m?)])
  (:import clojure.lang.ExceptionInfo))
