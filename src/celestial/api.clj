(ns celestial.api
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [clojure.core.strint :only (<<)]
        [slingshot.slingshot :only  [throw+ try+]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [ring.middleware.params :only (wrap-params)]
        [metrics.ring.instrument :only  (instrument)]
        [swag.core :only (swagger-routes GET- POST- PUT- DELETE- defroutes-)]
        [swag.model :only (defmodel wrap-swag defv defc)]
        [celestial.common :only (import-logging)]
        )
  (:require 
    [celestial.security :as sec]
    [celestial.persistency :as p]
    [compojure.handler :refer (site)]
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [cemerick.friend :as friend]
    [compojure.route :as route])) 

(import-logging)

(def ^{:doc "see http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html"}
  http-codes {:conflict 409 :success 200 :bad-req 400})

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

(defv [:proxmox :type]
  (let [allowed (get-in proxmox [:properties :type :allowableValues :values])]
    (when-not (first (filter #{v} allowed))
      (throw (clojure.lang.ExceptionInfo. (<< "Value ~{v} for proxmox type isn't valid") {:error :validation})))))

(defc [:proxmox :type] (keyword v))

(defc [:machine :os] (keyword v))

(defmodel aws :min-count :int :max-count :int :instance-type :string
  :image-id :string :keyname :string :endpoint :string)

(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}

  (POST- "/job/stage/:host" [^:string host] {:nickname "stageMachine" :summary "Complete staging job"}
         (jobs/enqueue "stage" {:identity host :args [(p/host host)]})
         (success {:msg "submitted staging" :host host}))

  (POST- "/job/create/:host" [^:string host] {:nickname "createMachine" :summary "Machine creation job"}
         (success 
           {:msg "submited system creation" :host host 
            :job (jobs/enqueue "machine" {:identity host :args [(p/host host)]})}))

  (POST- "/job/provision/:host" [^:string host] {:nickname "provisionHost" :summary "provisioning job"}
         (let [machine (p/host host) type (p/type-of (:type machine)) ]
           (success 
             {:msg "submitted provisioning" :host host :machine machine :type type 
              :job (jobs/enqueue "provision" {:identity host :args [type machine]})})))

  (GET- "/job/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (success {:job-status (jobs/status queue uuid)}))

  )

(defroutes- hosts {:path "/host" :description "Operations on hosts"}

  (GET- "/host/machine/:host" [^:string host] {:nickname "getHostMachine" :summary "Get Host machine"}
        (success (p/host host)))

  (POST- "/host/machine" [& ^:system props] {:nickname "addHostMachine" :summary "Add Host machine"}
         (let [host (get-in props [:machine :hostname])]
           (if (p/host-exists? host)
             (conflict {:msg "Host aleady exists, use PUT /host/machine instead"}) 
             (try+ 
               (p/register-host props)
               (success {:msg "new host saved" :host host :props props})
               (catch [:type :celestial.persistency/missing-type] e 
                 (bad-req  {:msg (<< "Cannot create machine with missing type ~(e :t)}")}))) 
             )))

  (PUT- "/host/machine" [& ^:system props] {:nickname "updateHostMachine" :summary "Add Host machine"}
        (let [host  (get-in props [:machine :hostname])]
          (when-not (p/host-exists? host)
            (conflict {:msg "Host does not exists, use POST /host/machine first"})) 
          (p/register-host props) 
          (success {:msg "new host saved" :host host :props props})))

  (DELETE- "/host/machine/:host" [^:string host] {:nickname "deleteHost" :summary "Delete Host"}
           (if (p/host-exists? host)
             (do (p/delete-host host) (success {:msg "Host deleted"}))
             (bad-req {:msg "Host does not exist"})))

  (GET- "/host/type/:host" [^:string host] {:nickname "getHostType" :summary "Fetch Host type"}
        (success (select-keys (p/type-of (:type (p/fuzzy-host host))) [:classes])))

  (POST- "/type" [^:string type & ^:type props] {:nickname "addType" :summary "Add type"}
         (p/new-type type props)
         (success {:msg "new type saved" :type type :opts props}))) 

(defroutes app-routes
  hosts jobs
  (route/not-found "Not Found"))

(defn error-wrap
  "A catch all error handler"
  [app]
  (fn [req]
    (try 
      (app req)
      (catch Throwable e {:body (.getMessage e) :status 500}))))

(defn app [secured?]
  "The api routes, secured? will enabled authentication"
  (-> (routes swagger-routes
              (if secured? (sec/secured-app app-routes) app-routes))
      (wrap-swag) 
      (handler/api)
      (wrap-restful-params) 
      (wrap-restful-response)
      (expose-metrics-as-json)
      (instrument)
      (error-wrap)
      ))
