(ns celestial.security
  (:use 
    [celestial.common :only (import-logging)])
  (:require 
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds])))

(import-logging)

(derive ::admin ::user)

; TODO move this into redis
(def users (atom 
        {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{::admin}}
         "ronen" {:username "ronen"
                    :password (creds/hash-bcrypt "user_password")
                    :roles #{::user}}}))

(def user #{::user})

(defn user-tracking [app]
  "A tiny middleware to track api access"
  (fn [{:keys [uri request-method] :as req}]
    (debug request-method " on " uri "by" (friend/current-authentication) )
    (app req)))

(defn secured-app [routes]
  (friend/authenticate 
    (friend/wrap-authorize (user-tracking routes) user) 
    {:allow-anon? true
     :unauthenticated-handler 
        #(assoc (workflows/http-basic-deny "celestial" %) :body {:message "login failed" } )
     :workflows [(workflows/http-basic
                   :credential-fn #(creds/bcrypt-credential-fn @users %)
                   :realm "celestial")]}))

