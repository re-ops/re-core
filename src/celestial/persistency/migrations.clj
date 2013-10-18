(ns celestial.persistency.migrations
  "Celestial global migrations"
 (:require 
   [celestial.security :refer (set-user)]
   [puny.migrations :refer (migrate)]
   [celestial.persistency.systems :as s]
   [celestial.persistency :as p]))

(defn register-all 
  "registers all global migrations"
   []
  (s/register-migrations))

(defn migrate-all
   "runs all global migrations" 
   []
  (migrate :systems))

(defn setup-migrations 
   "registers and runs migrations" 
   []
  (set-user (p/get-user "admin")
    (register-all)
    (migrate-all)))
