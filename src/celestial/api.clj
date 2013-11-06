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

(ns celestial.api
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.roles :refer (admin su)]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.params :refer (wrap-params)]
    [metrics.ring.instrument :refer  (instrument)]
    [swag.model :refer (defmodel wrap-swag)]
    [celestial.common :refer (import-logging get! version wrap-errors success)]
    [compojure.core :refer (defroutes routes)] 
    [clojure.core.strint :refer (<<)]
    [celestial.api.ui :refer (public sessions)]
    [celestial.api.users :refer (users quotas users-ro)]
    [celestial.persistency :as p]
    [celestial.security :as sec]
    [celestial.api.systems :refer (systems environments)]
    [celestial.api.types :refer (types)]
    [celestial.api.jobs :refer (jobs)]
    [ring.middleware.session.cookie :refer (cookie-store)]
    [ring.middleware.session :refer (wrap-session)]
    [compojure.core :refer (GET ANY)] 
    [swag.core :refer (swagger-routes GET- POST- PUT- DELETE- defroutes- errors )]
    [compojure.handler :as handler]
    [cemerick.friend :as friend]
    [compojure.route :as route]))

(import-logging)

(defmodel object)

(defmodel capistrano :args {:type "List" :items {"$ref" "String"}})

(defmodel ccontainer :capistrano {:type "Capistrano"} )

(defmodel actions :action-a {:type "Ccontainer"})

(defmodel action :operates-on :string :src :string :actions {:type "Actions"})

(defmodel arguments :args {:type "Hash" :description "key value pairs {'foo':1 , 'bar':2 , ...}" })


(defroutes- actions {:path "/actions" :description "Adhoc actions managment"}
  (POST- "/actions" [& ^:action action] {:nickname "addActions" :summary "Adds an actions set"}
    (wrap-errors (success {:msg "added actions" :id (p/add-action action)})))

  (PUT- "/actions/:id" [^:int id & ^:action action] {:nickname "updateActions" :summary "Update an actions set"}
        (wrap-errors
          (p/update-action id action)
           (success {:msg "updated actions" :id id})))

  (GET- "/actions/type/:type" [^:string type] {:nickname "getActionsByTargetType" :summary "Gets actions that operate on a target type"}
        (let [ids (p/get-action-index :operates-on type)]
           (success (apply merge {} (map #(hash-map % (p/get-action %)) ids)))))

  (GET- "/actions/:id" [^:int id] {:nickname "getActions" :summary "Gets actions descriptor"}
        (success (p/get-action id)))

  (DELETE- "/actions/:id" [^:int id] {:nickname "deleteActions" :summary "Deletes an action set"}
           (p/delete-action id)
           (success {:msg "Deleted action" :id id})))

(defroutes app-routes
  systems types environments actions jobs sessions 
  (friend/wrap-authorize users-ro su)
  (friend/wrap-authorize users admin)
  (friend/wrap-authorize quotas admin)
  (route/not-found "Not Found"))

(defn error-wrap
  "A catch all error handler"
  [app]
  (fn [req]
    (try 
      (app req)
      (catch Throwable e 
        (error e)
        {:body (<< "Unexpected error ~(.getMessage e) of type ~(class e) contact celestial admin for more info") :status 500}))))

(defn force-https [rs]
  (binding [friend/*default-scheme-ports* {:http (get! :celestial :port) :https (get! :celestial :https-port)}]
    (friend/requires-scheme rs :https)))

(defn compose-routes
  "Composes celetial apps" 
  [secured?]
  (let [rs (routes public (swagger-routes version) (if secured? (sec/secured-app app-routes) app-routes) )]
    (if secured? 
      (force-https rs) rs)))

(defn app [secured?]
  "The api routes, secured? will enabled authentication"
  (-> (compose-routes secured?) 
      (wrap-swag) 
      (wrap-session {:cookie-name "celestial" :store (cookie-store)})
      (wrap-restful-format :formats [:json-kw :edn :yaml-kw :yaml-in-html])
      (handler/api)
      (instrument)
      (error-wrap)))
