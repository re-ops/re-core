(ns celestial.api
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [metrics.ring.instrument :only  (instrument)]
        [celestial.swagger :only (swagger-routes GET- POST- defroutes- defmodel)]
        [taoensso.timbre :only (debug info error warn set-config! set-level!)])
  (:require 
    [celestial.security :as sec]
    [celestial.persistency :as p]
    [compojure.handler :refer (site)]
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [cemerick.friend :as friend]
    [compojure.route :as route])) 

(set-config! [:shared-appender-config :spit-filename ] "celestial.log"); TODO move this to /var/log
(set-config! [:appenders :spit :enabled?] true)
(set-level! :trace)

(defn generate-response [data] {:status 200 :body data})

(defmodel type :puppet-std {:type "Puppetstd"})

(defmodel puppetstd :module {:type "Module"} :type :string :classes {:type "Classes"} )

(defmodel module :name :string :src :string)

(defmodel classes)


(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}
  (POST- "/job/stage/:host" [^:string host] {:nickname "stageMachine" :summary "Complete staging job"}
         (jobs/enqueue "stage" {:identity host :args [(p/host host)]})
         (generate-response {:msg "submitted staging" :host host}))

  (POST- "/job/create/:host" [^:string host] {:nickname "createMachine" :summary "Machine creation job"}
         (jobs/enqueue "machine" {:identity host :args [(p/host host)]})
         (generate-response {:msg "submited system creation" :host host}))

  (POST- "/job/provision/:host" [^:string host] {:nickname "provisionHost" :summary "provisioning job"}
         (let [machine (p/host host) type (p/type-of (:type machine))]
           (jobs/enqueue "provision" {:identity host :args [type machine]}) 
           (generate-response {:msg "submitted provisioning" :host host :machine machine :type type}))))

(defroutes- hosts {:path "/host" :description "Operations on hosts"}
  (GET- "/host/machine/:host" [^:string host] {:nickname "getHostMachine" :summary "Host machine"}
        (generate-response (p/host host)))

  (POST "/host/machine" [& props] {:nickname "getHostMachine" :summary "Host machine"}
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
      (handler/api)
      (wrap-restful-params)
      (wrap-restful-response)
      (expose-metrics-as-json)
      (instrument)
      ))
