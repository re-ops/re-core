(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-conf is-type?)]
    [celestial.fixtures.populate :refer (re-initlize)]
    [celestial.persistency.users :as u])
  (:use midje.sweet))


(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "generated crud user ops" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (u/add-user user)]
            (u/get-user id) => user
            (u/user-exists? id) => truthy
            (u/update-user (merge user {:username "foo" :password "123"}))
            (u/get-user id) => (merge user {:username "foo" :password "123"})
            (u/delete-user id)
            (u/user-exists? id) => falsey))

    (fact "non valid user" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (u/add-user user)]
            (u/add-user (dissoc user :username)) => 
            (throws ExceptionInfo (is-type? :celestial.persistency.users/non-valid-user))
            (u/update-user (dissoc user :username)) =>
            (throws ExceptionInfo (is-type? :celestial.persistency.users/non-valid-user))))))

