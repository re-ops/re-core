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
  (:use [compojure.core :only (defroutes context POST GET routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [celestial.roles :only (roles roles-m admin)]
        [clojure.core.strint :only (<<)]
        [slingshot.slingshot :only  [throw+ try+]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [ring.middleware.params :only (wrap-params)]
        [metrics.ring.instrument :only  (instrument)]
        [swag.core :only (swagger-routes http-codes GET- POST- PUT- DELETE- defroutes- errors)]
        [swag.model :only (defmodel wrap-swag defv defc)]
        [celestial.common :only (import-logging get*)])
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

(defn resp
  "Http resposnse compositor"
  [code data] {:status (http-codes code) :body data})

(def bad-req (partial resp :bad-req))
(def conflict (partial resp :conflict))
(def success (partial resp :success))

(defmodel type :type :string :puppet-std {:type "Puppetstd"} :classes {:type "Object"})

(defmodel puppetstd :module {:type "Module"})

(defmodel module :name :string :src :string)

(defmodel object)

(defmodel system 
  :machine {:type "Machine"} 
  :aws {:type "Aws" :description "Only for ec2"}
  :proxmox {:type "Proxmox" :description "Only for proxmox"}
  :type :string)

(defmodel machine 
  :cpus {:type :int :description "Not relevant in ec2"}
  :memory {:type :int :description "Not relevant in ec2"}
  :disk {:type :int :description "Not relevant in ec2"}
  :hostname :string :user :string :os :string :ip {:type :string :description "Not relevant in ec2"})

(defmodel proxmox :nameserver :string :searchdomain :string :password :string :node :string 
  :type {:type :string :allowableValues {:valueType "LIST" :values ["ct" "vm"]}}
  :features {:type "List"})

(defmodel user :username :string :password :string 
  :roles {:type :string :allowableValues {:valueType "LIST" :values (into [] (keys roles-m))}})

(defv [:proxmox :type]
  (let [allowed (get-in proxmox [:properties :type :allowableValues :values])]
    (when-not (first (filter #{v} allowed))
      (throw (clojure.lang.ExceptionInfo. (<< "Value ~{v} for proxmox type isn't valid") {:error :validation})))))

(defc [:proxmox :type] (keyword v))

(defc [:machine :os] (keyword v))

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
         (let [system (p/get-system id) type (p/type-of (:type system)) 
               job (jobs/enqueue "provision" {:identity id :args [type system]})]
           (println job)
           (success 
             {:msg "submitted provisioning" :id id :machine machine :type type :job job})))

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

(defroutes- users {:path "/user" :description "User managment"}
  (GET- "/user/:name" [^:string name] {:nickname "getUser" :summary "Get User"}
        (success (p/get-user name)))

  (POST- "/user/" [& ^:user user] {:nickname "addUser" :summary "Adds a new user"}
         (p/add-user (-> user convert-roles hash-pass))
         (success {:msg "added user"}))

  (PUT- "/user/" [& ^:user user] {:nickname "updateUser" :summary "Updates an existing user"}
        (p/update-user (-> user convert-roles hash-pass))
        (success {:msg "user updated"}))

  (DELETE- "/user/:name" [^:string name] {:nickname "deleteUser" :summary "Deleted a user"}
           (p/delete-user name) 
           (success {:msg "user deleted" :name name})))

(defroutes- hosts {:path "/host" :description "Operations on hosts"}

  (GET- "/host/system/:id" [^:int id] {:nickname "getSystem" :summary "Get system by id"}
        (success (p/get-system id)))

  (POST- "/host/system" [& ^:system props] {:nickname "addSystem" :summary "Add system" 
                                            :errorResponses (errors {:bad-req "Missing system type"})}
         (try+ 
           (let [id (p/add-system props)]
             (success {:msg "new system saved" :id id :props props})) 
           (catch [:type :celestial.persistency/missing-type] e 
             (bad-req {:msg (<< "Cannot create machine with missing type ~(e :t)}")}))))

  (POST- "/host/system-clone/:id" [^:int id] {:nickname "cloneSystem" :summary "Clone an existing system replacing unique identifiers along the way" 
                                         :errorResponses (errors {:bad-req "System missing"})}
         (if-not (p/system-exists? id)
           (conflict {:msg "System does not exists, use POST /host/system to create it first"}) 
           (let [clone-id (p/clone-system id)]  
             (success {:msg "system cloned" :id clone-id}))))

  (PUT- "/host/system/:id" [^:int id & ^:system system] {:nickname "updateSystem" :summary "Update system" 
                                                         :errorResponses (errors {:conflict "System does not exist"}) }
        (if-not (p/system-exists? id)
          (conflict {:msg "System does not exists, use POST /host/system first"}) 
          (do (p/update-system id system) 
              (success {:msg "system updated" :id id}))))

  (DELETE- "/host/system/:id" [^:int id] {:nickname "deleteSystem" :summary "Delete System" 
                                          :errorResponses (errors {:bad-req "System does not exist"})}
           (if (p/system-exists? id)
             (do (p/delete-system id) 
                 (success {:msg "System deleted"}))
             (bad-req {:msg "Host does not exist"})))

  (GET- "/host/type/:id" [^:int id] {:nickname "getSystemType" :summary "Fetch type of provided system id"}
        (success (select-keys (p/type-of (:type (p/get-system id))) [:classes])))

  (POST- "/type" [^:string type & ^:type props] {:nickname "addType" :summary "Add type"}
         (p/new-type type props)
         (success {:msg "new type saved" :type type :opts props}))) 

(defroutes app-routes
  hosts tasks jobs (friend/wrap-authorize users admin) (route/not-found "Not Found"))

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
