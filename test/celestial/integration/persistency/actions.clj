(ns celestial.integration.persistency.actions
 (:import clojure.lang.ExceptionInfo)
 (:require 
  [celestial.fixtures.data :refer (redis-prox-spec redis-type user-quota redis-deploy)]
  [celestial.fixtures.core :refer (is-type? with-conf)]
  [celestial.persistency.types :as t]
  [celestial.fixtures.populate :refer (re-initlize)]
  [celestial.persistency.actions :as a])
 (:use midje.sweet))

(def redis-deploy-provided (assoc redis-deploy :provided '()))

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
   (fact "basic actions usage" :integration :redis :actions
     (t/add-type redis-type) 
     (let [id (a/add-action redis-deploy)]
       (a/get-action id) => redis-deploy-provided
       (a/get-action-index :operates-on "redis") => [(str id)]
       (a/find-action-for "deploy" "redis") =>  redis-deploy-provided))
 
   (fact "duplicated actions" :integration :redis :actions
     (t/add-type redis-type) 
     (let [id (a/add-action redis-deploy)]
       (a/get-action id) => redis-deploy-provided
       (a/add-action redis-deploy) => 
          (throws ExceptionInfo (is-type? :celestial.persistency.actions/duplicated-action))))))
