(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns celestial.security
  (:use 
    [celestial.common :only (import-logging)])
  (:require 
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds])))

(import-logging)

(def roles {"admin" ::admin "user" ::user "anonymous" ::anonymous})

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

