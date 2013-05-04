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
  (:use [celestial.hosts-api :only (hosts types)]
        [celestial.users-api :only (users)]
        [compojure.core :only (defroutes routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [celestial.roles :only (roles roles-m admin)]
        [clojure.core.strint :only (<<)]
        [slingshot.slingshot :only  [throw+ try+]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [ring.middleware.params :only (wrap-params)]
        [metrics.ring.instrument :only  (instrument)]
        [swag.core :only (swagger-routes GET- POST- PUT- DELETE- defroutes- errors)]
        [swag.model :only (defmodel wrap-swag defv defc)]
        [celestial.common :only (import-logging get* resp bad-req conflict success)])
  (:require 
    [ring.middleware [multipart-params :as mp] ]
    [celestial.security :as sec]
    [celestial.persistency :as p]
    [compojure.handler :as handler :refer (site)]
    [celestial.jobs :as jobs]
    [cemerick.friend :as friend]
    [cemerick.friend.credentials :as creds]
    [compojure.route :as route])) 

(import-logging)

(defmodel module :name :string :src :string)

(defmodel object)

(defmodel aws :min-count :int :max-count :int :instance-type :string
  :image-id :string :key-name :string :endpoint :string)

(defmodel capistrano :name :string :src :string :args :string)

(defmodel task :capistrano {:type "Capistrano" :description "For capistrano based tasks"})

(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}

  (POST- "/job/stage/:id" [^:int id] {:nickname "stageSystem" :summary "Complete end to end staging job"}
         (jobs/enqueue "stage" {:identity id :args [(p/get-system id)]})
         (success {:msg "submitted staging" :id id}))

  (POST- "/job/create/:id" [^:int id] {:nickname "createSystem" :summary "System creation job"
                                       :errorResponses (errors {:bad-req "Missing system"})}
         (if-not (p/system-exists? id)
           (bad-req {:msg (<< "No system found with given id ~{id}")})
           (success 
             {:msg "submited system creation" :id id 
              :job (jobs/enqueue "reload" 
              {:identity id :args [(assoc (p/get-system id) :system-id (Integer. id))]})})))

  (POST- "/job/destroy/:id" [^:int id] {:nickname "destroySystem" :summary "System destruction job"}
         (success 
           {:msg "submited system destruction" :id id 
            :job (jobs/enqueue "destroy" {:identity id :args [(p/get-system id)]})}))

  (POST- "/job/provision/:id" [^:int id] {:nickname "provisionSystem" :summary "Provisioning job"}
         (let [system (p/get-system id) type (p/get-type (:type system)) 
               job (jobs/enqueue "provision" {:identity id :args [type system]})]
           (success 
             {:msg "submitted provisioning" :id id :system system :type type :job job})))

  (GET- "/job/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (success {:job-status (jobs/status queue uuid)})))


(defn convert-roles [user]
  (update-in user [:roles] (fn [v] #{(roles-m v)})))

(defn hash-pass [user]
  (update-in user [:password] (fn [v] (creds/hash-bcrypt v))))

(defroutes- tasks {:path "/tasks" :description "Tasks managment"}
  (POST- "/task" [& ^:task task] {:nickname "addTask" :summary "Adds a new task"}
         (let [id (p/add-task task)]
           (success {:msg "added new task" :id id})))

  (PUT- "/task/:id" [^:int id & ^:task task] {:nickname "updateTask" :summary "Update a task"}
        (p/update-task id task)
        (success {:msg "updated task" :task task}))

  (GET- "/task/:id" [^:int id] {:nickname "getTask" :summary "Get a task"}
        (success {:task (p/get-task id)}))

  (DELETE- "/task/:id" [^:int id] {:nickname "deleteTask" :summary "Deletes a task"}
           (p/delete-task id)
           (success {:msg "delete task" :id id})))



(defroutes app-routes
  hosts types tasks jobs (friend/wrap-authorize users admin) (route/not-found "Not Found"))

(defn error-wrap
  "A catch all error handler"
  [app]
  (fn [req]
    (try 
      (app req)
      (catch Throwable e {:body (.getMessage e) :status 500}))))

(defn force-https [rs]
  (binding [friend/*default-scheme-ports* {:http (get* :celestial :port) :https (get* :celestial :https-port)}]
    (friend/requires-scheme rs :https)))

(defn compose-routes
  "Composes celetial apps" 
  [secured?]
  (let [rs (routes (swagger-routes) (if secured? (sec/secured-app app-routes) app-routes))]
    (if secured? 
      (force-https rs) rs)))

(defn app [secured?]
  "The api routes, secured? will enabled authentication"
  (-> (compose-routes secured?) 
      (wrap-swag) 
      (handler/api)
      (wrap-restful-params) 
      (wrap-restful-response)
      (mp/wrap-multipart-params)
      (expose-metrics-as-json)
      (instrument)
      (error-wrap)))
