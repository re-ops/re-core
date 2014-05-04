(ns celestial.integration.persistency.migrations
  "Testing data migrations"
  (:require 
    [taoensso.carmine :as car]
    [cemerick.friend :as friend]
    [celestial.fixtures.data :refer (redis-prox-spec redis-type)]
    [celestial.fixtures.core :refer (is-type? with-conf)]
    [celestial.fixtures.populate :refer (add-users re-initlize)]
    [celestial.redis :refer (wcar)]  
    [celestial.persistency.migrations :as m]
    [celestial.persistency.systems :as s]
    [celestial.persistency.users :as u])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))

; TODO test some migrations, these test seem to be out of date
#_(with-conf
  (with-state-changes [(before :facts (re-initlize))]
   (fact "user envs migration" :integration :migration :redis
      (u/add-user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}) => "foo"
      (u/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}
      (wcar (car/hset (u/user-id "foo") :meta {:version nil}))
      (u/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs [:dev]}))

  (against-background 
   [(friend/current-authentication) => {:identity "admin" :username "admin"} ]
    (with-state-changes 
      [(before :facts (do (re-initlize) (u/add-type redis-type) (add-users) (m/register-all)))]
      (fact "system env index migration" :integration :migration :redis
        (let [id (s/add-system redis-prox-spec)]
              (wcar (car/hset (s/system-id id) :meta {:version nil}))
              (wcar (car/srem "system:env:dev" id))
              (wcar (car/smembers "system:env:dev")) => []
              (m/migrate-all) 
              (s/get-system id) => redis-prox-spec
              (:version (meta (s/get-system id)))  => 1
              (s/get-system-index :env :dev) => [(str id)]
              (wcar (car/smembers "system:env:dev")) => [(str id)]
              (wcar (car/srem "system:env:dev" id))
              (m/migrate-all) ; migrations should only run once
              (wcar (car/smembers "system:env:dev")) => [])))))
