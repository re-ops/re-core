(ns celestial.integration.persistency.actions
 (:import clojure.lang.ExceptionInfo)
 (:require 
  [celestial.fixtures.data :refer (redis-prox-spec redis-type user-quota redis-deploy)]
  [celestial.fixtures.core :refer (is-type?)]
  [celestial.persistency :as p]
  [celestial.redis :refer (clear-all)]  
  [celestial.persistency.actions :as a])
 (:use midje.sweet))

(def redis-deploy-provided (assoc redis-deploy :provided nil))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic actions usage" :integration :redis :actions
    (p/add-type redis-type) 
    (let [id (a/add-action redis-deploy)]
      (a/get-action id) => redis-deploy-provided
      (a/get-action-index :operates-on "redis") => [(str id)]
      (a/find-action-for "deploy" "redis") =>  redis-deploy-provided))

  (fact "duplicated actions" :integration :redis :actions
    (p/add-type redis-type) 
    (let [id (a/add-action redis-deploy)]
      (a/get-action id) => redis-deploy-provided
      (a/add-action redis-deploy) => 
         (throws ExceptionInfo (is-type? :celestial.persistency.actions/duplicated-action)))))
