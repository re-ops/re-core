(ns celestial.test.actions
  (:require 
    [celestial.model :refer (rconstruct)]
    [celestial.persistency.actions :refer (validate-action)]
    [celestial.persistency.common :refer (args-of)]
    [celestial.persistency.types :refer (type-exists?)]
    [celestial.fixtures.data :refer (redis-deploy)] 
    [celestial.fixtures.core :refer (with-m?)] 
    remote.capistrano)
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(fact "actions to remoter construction"
  (rconstruct redis-deploy {:target "192.168.5.31" :hostname "foo" :env :dev}) =>
    (contains {:args ["deploy" "-s" "hostname=192.168.5.31"]
               :src  "git://github.com/narkisr/cap-demo.git"}))

(fact "action validations"
  (with-redefs [type-exists? (fn [_] true)]
    (validate-action redis-deploy)  => truthy
    (validate-action (assoc-in redis-deploy [:capistrano :dev :args] nil)) =>
    (throws ExceptionInfo (with-m? '{:capistrano ({:dev {:args "must be present"}})})) 
    (validate-action (assoc-in redis-deploy [:actions :stop :supernal :args] nil)) => truthy)

   (fact "missing type" 
     (validate-action (assoc redis-deploy :operates-on "foo")) =>
     (throws ExceptionInfo (with-m? {:operates-on  "type not found, create it first"})) 
     (provided (type-exists? anything)  => false)))

(fact "args parsing"
   (fact "sanity" 
     (args-of "deploy ~{a} ~{b}") => #{"a" "b"})
   (fact "repeating args"
     (args-of "~{a} ~{b} ~{a}") => #{"a" "b"}))
