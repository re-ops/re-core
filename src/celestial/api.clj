(ns celestial.api
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET routes)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [metrics.ring.instrument :only  (instrument)]
        [celestial.swagger :only (swagger-routes GET- POST- defroutes-)]
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

(defroutes provision-routes
  (POST "/:host" [host] 
        (let [machine (p/host host) type (p/type-of (:type machine))]
          (jobs/enqueue "provision" {:identity host :args [type machine]}) 
          (generate-response {:msg "submitted provisioning" :host host :machine machine :type type}))))

(defroutes stage-routes
  (POST "/:host" [host] 
        (jobs/enqueue "stage" {:identity host :args [(p/host host)]})
        (generate-response {:msg "submitted staging" :host host})))

(defroutes- jobs {:path "/job" :description "Operations on async job scheduling"}
  (POST- "/job/:host" [^:string host] {:nickname "createMachine" :summary "schedules a machine creation job"}
        (jobs/enqueue "machine" {:identity host :args [(p/host host)]})
        (generate-response {:msg "submited system creation" :host host})))

(defroutes- hosts {:path "/host" :description "Operations on hosts"}
  (POST "/host" [& props]
        (p/register-host props)
        (generate-response {:msg "new host saved" :host (get-in props [:machine :hostname]) :props props}))
  (GET- "/host/machine/:host" [^:string host] {:nickname "getHostMachine" :summary "Host machine"}
        (generate-response (p/host host)))
  (GET- "/host/type/:host" [^:string host] {:nickname "getHostType" :summary "Host type"}
       (generate-response (select-keys (p/type-of (:type (p/fuzzy-host host))) [:classes])))
  (POST "/type" [type & props]
        (p/new-type type props)
        (generate-response {:msg "new type saved" :type type :opts props}))) 


(defroutes app-routes
  (context "/stage" [] stage-routes)
  (context "/provision" [] provision-routes)
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
