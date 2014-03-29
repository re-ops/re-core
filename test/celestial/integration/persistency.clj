(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.core :refer (with-conf is-type?)]
    [celestial.fixtures.populate :refer (re-initlize)]
    [celestial.persistency :as p])
  (:use midje.sweet))


(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "generated crud user ops" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (p/add-user user)]
            (p/get-user id) => user
            (p/user-exists? id) => truthy
            (p/update-user (merge user {:username "foo" :password "123"}))
            (p/get-user id) => (merge user {:username "foo" :password "123"})
            (p/delete-user id)
            (p/user-exists? id) => falsey))

    (fact "non valid user" :integration :redis
          (let [user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []} id (p/add-user user)]
            (p/add-user (dissoc user :username)) => 
            (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user))
            (p/update-user (dissoc user :username)) =>
            (throws ExceptionInfo (is-type? :celestial.persistency/non-valid-user))))))

