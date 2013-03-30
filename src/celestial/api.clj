(ns celestial.api
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [ring.middleware.params :only (wrap-params)]
        [metrics.ring.instrument :only  (instrument)]
        [swag.core :only (swagger-routes GET- POST- defroutes-)]
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

(defn generate-response [data] {:status 200 :body data})

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
      (throw (Exception. (str v " proxmox type isn't allowed"))))))

(defc [:proxmox :type] (keyword v))

(defc [:machine :os] (keyword v))

(defmodel aws :min-count :int :max-count :int :instance-type :string
  :image-id :string :keyname :string :endpoint :string)

(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}

  (POST- "/job/stage/:host" [^:string host] {:nickname "stageMachine" :summary "Complete staging job"}
         (jobs/enqueue "stage" {:identity host :args [(p/host host)]})
         (generate-response {:msg "submitted staging" :host host}))

  (POST- "/job/create/:host" [^:string host] {:nickname "createMachine" :summary "Machine creation job"}
         (generate-response 
           {:msg "submited system creation" :host host 
            :job (jobs/enqueue "machine" {:identity host :args [(p/host host)]})}))

  (POST- "/job/provision/:host" [^:string host] {:nickname "provisionHost" :summary "provisioning job"}
         (let [machine (p/host host) type (p/type-of (:type machine)) ]
           (generate-response 
             {:msg "submitted provisioning" :host host :machine machine :type type 
              :job (jobs/enqueue "provision" {:identity host :args [type machine]})})))

  (GET- "/job/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (generate-response {:job-status (jobs/status queue uuid)}))

  )

(defroutes- hosts {:path "/host" :description "Operations on hosts"}
  (GET- "/host/machine/:host" [^:string host] {:nickname "getHostMachine" :summary "Get Host machine"}
        (generate-response (p/host host)))

  (POST- "/host/machine" [& ^:system props] {:nickname "getHostMachine" :summary "Add Host machine"}
         (p/register-host props)
         (generate-response 
           {:msg "new host saved" :host (get-in props [:machine :hostname]) :props props}))

  (GET- "/host/type/:host" [^:string host] {:nickname "getHostType" :summary "Host type"}
        (generate-response (select-keys (p/type-of (:type (p/fuzzy-host host))) [:classes])))

  (POST- "/type" [^:string type & ^:type props] {:nickname "addType" :summary "Add type"}
         (p/new-type type props)
         (generate-response {:msg "new type saved" :type type :opts props}))) 

(defroutes app-routes
  hosts jobs
  (route/not-found "Not Found"))

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
      ))
