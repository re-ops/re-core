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
  (:refer-clojure :exclude [hash type])
  (:use 
    [celestial.hosts-api :only (system type environments)]
    [celestial.users-api :only (users quotas)]
    [celestial.ui-api :only (public sessions)]
    [gelfino.timbre :only (get-tid)]
    [compojure.core :only (defroutes routes)] 
    [metrics.ring.expose :only  (expose-metrics-as-json)]
    [celestial.roles :only (roles roles-m admin)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+ try+]]
    [ring.middleware.format :only [wrap-restful-format]]
    [ring.middleware.params :only (wrap-params)]
    [metrics.ring.instrument :only  (instrument)]
    [swag.model :only (defmodel wrap-swag defv defc)]
    [celestial.common :only (import-logging get! resp bad-req conflict success version wrap-errors)])
  (:require 
    [ring.middleware.session.cookie :refer (cookie-store)]
    [ring.middleware.session :refer (wrap-session)]
    [compojure.core :refer (GET ANY)] 
    [swag.core :refer (swagger-routes GET- POST- PUT- DELETE- defroutes- errors )]
    [ring.middleware [multipart-params :as mp] ]
    [celestial.security :as sec]
    [celestial.persistency :as p]
    [compojure.handler :as handler :refer (site)]
    [celestial.jobs :as jobs]
    [cemerick.friend :as friend]
    [compojure.route :as route])) 

(import-logging)

(defmodel object)

(defmodel capistrano :args {:type "List" :items {"$ref" "String"}})

(defmodel ccontainer :capistrano {:type "Capistrano"} )

(defmodel actions :action-a {:type "Ccontainer"})

(defmodel action :operates-on :string :src :string :actions {:type "Actions"})

(defmodel hash)

(defmodel arguments :args {:type "Hash" :description "key value pairs {'foo':1 , 'bar':2 , ...}" })

(defn schedule-job 
  ([id action msg]
    (schedule-job id action msg [(assoc (p/get-system id) :system-id (Integer. id))])) 
  ([id action msg args]
    (if-not (p/system-exists? id)
     (bad-req {:errors (<< "No system found with given id ~{id}")})
     (success 
       {:msg msg :id id :job (jobs/enqueue action {:identity id :args args :tid (get-tid) :env (p/get-system id :env)})}))))

(defroutes- jobs {:path "/jobs" :description "Async job scheduling"}

  (POST- "/jobs/stage/:id" [^:int id] 
    {:nickname "stageSystem" :summary "Complete end to end staging job"
     :notes "Combined system creation and provisioning, separate actions are available also."}
      (let [system (p/get-system id) type (p/get-type (:type system))]
           (schedule-job id "stage" "submitted system staging" [type (assoc system :system-id (Integer. id))])))

  (POST- "/jobs/create/:id" [^:int id] 
     {:nickname "createSystem" :summary "System creation job"
      :errorResponses (errors {:bad-req "Missing system"})
      :notes "Creates a new system on remote hypervisor (usually followed by provisioning)."}
         (schedule-job id "reload" "submitted system creation"))

  (POST- "/jobs/destroy/:id" [^:int id] 
    {:nickname "destroySystem" :summary "System destruction job"
     :notes "Destroys a system, clearing it both from Celestial's model storage and hypervisor"}
         (schedule-job id "destroy" "submitted system destruction"))

  (POST- "/jobs/provision/:id" [^:int id] 
    {:nickname "provisionSystem" :summary "Provisioning job"
     :notes "Starts a provisioning workflow on a remote system
             using the provisioner configured in system type"}
         (let [system (p/get-system id) type (p/get-type (:type system))]
           (schedule-job id "provision" "submitted provisioning" 
              [type (assoc system :system-id (Integer. id))])))

  (POST- "/jobs/:action/:id" [^:string action ^:int id & ^:hash args] 
     {:nickname "runAction" :summary "Run remote action" 
      :notes "Runs adhoc remote opertions on system (like deployment, service restart etc)
              using matching remoting capable tool like Capisrano/Supernal/Fabric"}
       (let [{:keys [machine] :as system} (p/get-system id)]
         (if-let [actions (p/find-action-for (keyword action) (:type system))]
           (schedule-job id "run-action" (<< "submitted ~{action} action") 
             [actions (merge args {:action (keyword action) :hostname (machine :hostname) :target (machine :ip) :system-id (Integer. id)})])
           (bad-req {:msg (<< "No action ~{action} found for id ~{id}")})
           )))

  (GET- "/jobs/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "single job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (success {:job (jobs/status queue uuid)}))
    
  (GET- "/jobs" []
        {:nickname "jobsStatus" :summary "Global job status tracking" 
         :notes "job status can be either pending, processing, done or nil"}
        (success {:jobs (jobs/jobs-status)}))
  )

(defroutes- actions {:path "/actions" :description "Adhoc actions managment"}
  (POST- "/actions" [& ^:action action] {:nickname "addActions" :summary "Adds an actions set"}
    (wrap-errors (success {:msg "added actions" :id (p/add-action action)})))

  (PUT- "/actions/:id" [^:int id & ^:action action] {:nickname "updateActions" :summary "Update an actions set"}
        (wrap-errors
          (p/update-action id action)
           (success {:msg "updated actions" :id id})))

  (GET- "/actions/type/:type" [^:string type] {:nickname "getActionsByTargetType" :summary "Gets actions that operate on a target type"}
        (let [ids (p/get-action-index :operates-on type)]
           (success (apply merge (map #(hash-map % (p/get-action %)) ids)))))

  (GET- "/actions/:id" [^:int id] {:nickname "getActions" :summary "Gets actions descriptor"}
        (success (p/get-action id)))

  (DELETE- "/actions/:id" [^:int id] {:nickname "deleteActions" :summary "Deletes an action set"}
           (p/delete-action id)
           (success {:msg "Deleted action" :id id})))

(defroutes app-routes
  system type environments sessions actions jobs sessions (friend/wrap-authorize users admin)
  (friend/wrap-authorize quotas admin) (route/not-found "Not Found"))

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
      (handler/api)
      (wrap-restful-format :formats [:json-kw :edn :yaml-kw :yaml-in-html])
      (mp/wrap-multipart-params)
      (expose-metrics-as-json)
      (instrument)
      (error-wrap)))
