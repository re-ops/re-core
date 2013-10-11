(ns celestial.integration.migrations
  "Testing data migrations"
  (:require 
    [taoensso.carmine :as car]
    [cemerick.friend :as friend]
    [celestial.fixtures :refer (redis-prox-spec redis-type is-type? )]
    [celestial.redis :refer (clear-all wcar)]  
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
  [(friend/current-authentication) => {:identity "ronen" :username "ronen"}
   (p/get-user! "ronen") => {:envs [:dev :qa] :roles #{:celestial.roles/user} :username "ronen"}]
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type)))]
    (fact "system env index migration" :integration :migration
       (let [id (p/add-system redis-prox-spec)]
            (wcar (car/hset (p/system-id id) :meta {:version nil}))
            (wcar (car/srem "system:env:dev" id))
            (wcar (car/smembers "system:env:dev")) => []
            (p/get-system id)  => redis-prox-spec
            (:version (meta (p/get-system id)))  => 1
            (p/get-system-index :env :dev) => [(str id)]
            (wcar (car/smembers "system:env:dev")) => [(str id)]
          ))))

