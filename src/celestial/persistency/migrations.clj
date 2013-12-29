(ns celestial.persistency.migrations
  "Celestial global migrations"
 (:require 
   [celestial.security :refer (set-user)]
   [puny.migrations :refer (migrate)]
   [celestial.persistency.systems :as s]
  [celestial.persistency.actions :as a]
   [celestial.persistency :as p]))

(defn register-all 
  "registers all global migrations"
   []
  (a/register-migrations)
  (s/register-migrations))

(defn migrate-all
   "runs all global migrations" 
   []
  (migrate :systems)
  (migrate :actions))

(defn setup-migrations 
   "registers and runs migrations" 
   []
  (set-user (p/get-user "admin")
    (register-all)
    (migrate-all)))
