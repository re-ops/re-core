(ns celestial.integration.persistency.migrations
  "Testing data migrations"
  (:require 
    [taoensso.carmine :as car]
    [cemerick.friend :as friend]
    [celestial.fixtures :refer (redis-prox-spec redis-type is-type? add-users)]
    [celestial.redis :refer (clear-all wcar)]  
    [celestial.persistency.migrations :as m]
    [celestial.persistency.systems :as s]
    [celestial.persistency :as p])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet)
  )

(with-state-changes [(before :facts (clear-all))]
   (fact "user envs migration" :integration :migration
      (p/add-user {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}) => "foo"
      (p/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs []}
      (wcar (car/hset (p/user-id "foo") :meta {:version nil}))
      (p/get-user "foo") => {:username "foo" :password "bla" :roles #{:celestial.roles/user} :envs [:dev]}))

(against-background 
  [(friend/current-authentication) => {:identity "admin" :username "admin"} ]
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type) (add-users)))]
    (fact "system env index migration" :integration :migration
      (let [id (s/add-system redis-prox-spec)]
            (wcar (car/hset (s/system-id id) :meta {:version nil}))
            (wcar (car/srem "system:env:dev" id))
            (wcar (car/smembers "system:env:dev")) => []
            (m/migrate :systems) 
            (s/get-system id)  => redis-prox-spec
            (:version (meta (s/get-system id)))  => 1
            (s/get-system-index :env :dev) => [(str id)]
            #_(wcar (car/smembers "system:env:dev")) => [(str id)]
          )
       )))
