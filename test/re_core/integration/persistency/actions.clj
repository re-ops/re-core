(ns re-core.integration.persistency.actions
 (:import clojure.lang.ExceptionInfo)
 (:require
  [re-core.fixtures.data :refer (redis-type user-quota redis-deploy)]
  [re-core.fixtures.core :refer (is-type? with-conf)]
  [re-core.persistency.types :as t]
  [re-core.fixtures.populate :refer (re-initlize)]
  [re-core.persistency.actions :as a])
 (:use midje.sweet))

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
   (fact "basic actions usage" :integration :redis :actions
     (t/add-type redis-type)
     (let [id (a/add-action redis-deploy)]
       (a/get-action id) => redis-deploy
       (a/get-action-index :operates-on "redis") => [(str id)]
       (a/find-action-for "deploy" "redis") =>  redis-deploy))

   (fact "duplicated actions" :integration :redis :actions
     (t/add-type redis-type)
     (let [id (a/add-action redis-deploy)]
       (a/get-action id) => redis-deploy
       (a/add-action redis-deploy) =>
          (throws ExceptionInfo (is-type? :re-core.persistency.actions/duplicated-action))))))
