(ns celestial.persistency.migrations
  "Celestial global migrations"
  (:import java.security.SecureRandom )
  (:require 
    [cemerick.friend.credentials :as creds]
    [celestial.security :refer (set-user)]
    [puny.migrations :refer (migrate)]
    [celestial.persistency.systems :as s]
    [celestial.persistency.actions :as a]
    [celestial.roles :refer (system)]
    [es.migrations :as e]
    [celestial.persistency.users :as u]))

(defn register-all 
  "registers all global migrations"
  []
  (a/register-migrations)
  (s/register-migrations)
  (e/register-migrations))

(defn migrate-all
  "runs all global migrations" 
  []
  (migrate :systems)
  (migrate :actions)
  (migrate :systems-es)
  )

(defn migration-user 
   "This user is used for migrations he is a system user which means that he has access to all envs,
    A random password is generated for him making its login disabled in effect (unless admin will change it)."
   []
  (let [pass (.toString (BigInteger. 130 (SecureRandom.)) 32) 
        user {:username "migrations" :password (creds/hash-bcrypt pass) 
              :roles system :envs [] :operations []}]
    (when-not (u/user-exists? "migrations")
      (u/add-user user))))


(defn setup-migrations 
  "registers and runs migrations" 
  []
  (migration-user)
  (set-user (u/get-user "migrations")
    (register-all)
    (migrate-all)))
