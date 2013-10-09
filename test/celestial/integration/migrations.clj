(ns celestial.integration.migrations
  "Testing data migrations"
  (:require 
    [taoensso.carmine :as car]
    [celestial.redis :refer (clear-all wcar)]  
    [celestial.persistency :as p]
    )
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet)
  )

(with-state-changes [(before :facts (clear-all))]
   (fact "user migration" :integration :migration
      (p/add-user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}) => "foo"
      (p/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}
      (wcar (car/hset (p/user-id "foo") :meta {:version nil}))
      (p/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs [:dev]}))

