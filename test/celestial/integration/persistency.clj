(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.data :refer (foo)]
    [celestial.fixtures.core :refer (with-conf is-type?)]
    [celestial.fixtures.populate :refer (re-initlize)]
    [celestial.persistency.users :as u])
  (:use midje.sweet))


(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "generated crud user ops" :integration :redis
       (let [id (u/add-user foo)]
         (u/get-user id) => foo
         (u/user-exists? id) => truthy
         (u/update-user (merge foo {:username "foo" :password "123"}))
         (u/get-user id) => (merge foo {:username "foo" :password "123"})
         (u/delete-user id)
         (u/user-exists? id) => falsey))

    (fact "non valid user" :integration :redis
      (u/add-user (dissoc foo :username)) => 
      (throws ExceptionInfo (is-type? :celestial.persistency.users/non-valid-user))
      (u/update-user (dissoc foo :username)) =>
      (throws ExceptionInfo (is-type? :celestial.persistency.users/non-valid-user)))))

