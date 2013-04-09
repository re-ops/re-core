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

(defmodel proxmox :vmid :int :nameserver :string :searchdomain :string :password :string :node :string 
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
  :image-id :string :keyname :string :endpoint :string)

(defmodel capistrano :name :string :src :string :args :string)

(defmodel task :capistrano {:type "Capistrano" :description "For capistrano based tasks"})

(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}

  (POST- "/job/stage/:host" [^:string host] {:nickname "stageMachine" :summary "Complete end to end staging job"}
         (jobs/enqueue "stage" {:identity host :args [(p/host host)]})
         (success {:msg "submitted staging" :host host}))

  (POST- "/job/create/:host" [^:string host] {:nickname "createMachine" :summary "Machine creation job"}
         (success 
           {:msg "submited system creation" :host host 
            :job (jobs/enqueue "machine" {:identity host :args [(p/host host)]})}))

  (POST- "/job/provision/:host" [^:string host] {:nickname "provisionHost" :summary "Provisioning job"}
         (let [machine (p/host host) type (p/type-of (:type machine)) ]
           (success 
             {:msg "submitted provisioning" :host host :machine machine :type type 
              :job (jobs/enqueue "provision" {:identity host :args [type machine]})})))

  (GET- "/job/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (success {:job-status (jobs/status queue uuid)})))


(defn convert-roles [user]
   (update-in user [:roles] (fn [v] #{(roles-m v)})))

(defn hash-pass [user]
   (update-in user [:password] (fn [v] (creds/hash-bcrypt v))))

(defroutes- tasks {:path "/tasks" :description "Tasks managment"}
    (POST- "/task" [^:task script] {:nickname "addTask" :summary "adds a new task"}
         (debug (slurp (:tempfile script)))
         (success {:msg "added task"})))

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

  (GET- "/host/machine/:host" [^:string host] {:nickname "getHostMachine" :summary "Get Host machine"}
        (success (p/host host)))

  (POST- "/host/machine" [& ^:system props] {:nickname "addHostMachine" :summary "Add Host machine" 
                                             :errorResponses (errors {:conflict "Host already exists" :bad-req "Missing host type"})}
         (let [host (get-in props [:machine :hostname])]
           (if (p/host-exists? host)
             (conflict {:msg "Host aleady exists, use PUT /host/machine instead"}) 
             (try+ 
               (p/register-host props)
               (success {:msg "new host saved" :host host :props props})
               (catch [:type :celestial.persistency/missing-type] e 
                 (bad-req  {:msg (<< "Cannot create machine with missing type ~(e :t)}")}))) 
             )))

  (PUT- "/host/machine" [& ^:system props] {:nickname "updateHostMachine" :summary "Update Host machine" 
                                            :errorResponses (errors {:conflict "Host does not exist"}) }
        (let [host (get-in props [:machine :hostname])]
          (if-not (p/host-exists? host)
            (conflict {:msg "Host does not exists, use POST /host/machine first"}) 
            (do (p/register-host props) 
                (success {:msg "new host saved" :host host :props props})))))

  (DELETE- "/host/machine/:host" [^:string host] {:nickname "deleteHost" :summary "Delete Host" 
                                                  :errorResponses (errors {:bad-req "Host does not exist"})}
           (if (p/host-exists? host)
             (do (p/delete-host host) (success {:msg "Host deleted"}))
             (bad-req {:msg "Host does not exist"})))

  (GET- "/host/type/:host" [^:string host] {:nickname "getHostType" :summary "Fetch Host type"}
        (success (select-keys (p/type-of (:type (p/fuzzy-host host))) [:classes])))

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
