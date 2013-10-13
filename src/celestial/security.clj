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
    [celestial.common :only (gen-uuid)]
    [gelfino.timbre :only (set-tid)]
    [celestial.common :only (import-logging)])
  (:require 
    [celestial.roles :as roles]
    [celestial.persistency :as p]
    [clojure.core.strint :refer (<<)]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds])))

(import-logging)

(defn current-user 
   "Grab current user, by default uses friend" 
   []
   (friend/current-authentication))

(defn user-tracking [app]
  "A tiny middleware to track api access"
  (fn [{:keys [uri request-method] :as req}]
    (set-tid (gen-uuid)
      (debug request-method " on " uri "by" (current-user))
      (app req))))


(defn login-redirect
  [{:keys [form-params params] :as request}]
  (let [ user (java.net.URLEncoder/encode (or (get form-params "username") (:username params "")))
         param (<< "&login_failed=Y&user=~{user}")]
   (ring.util.response/redirect (<< "/login?~{param}"))))

(defn secured-app [routes]
  (friend/authenticate 
    (friend/wrap-authorize (user-tracking routes) roles/user) 
    {:allow-anon? true
     :credential-fn #(if (p/user-exists? (:username %)) (creds/bcrypt-credential-fn p/get-user! %) nil)
     :unauthenticated-handler #(assoc (workflows/http-basic-deny "basic-celestial" %) :body {:message "login failed" } )
     :workflows [(workflows/interactive-form :login-failure-handler login-redirect) (workflows/http-basic :realm "basic-celestial")]}))

