(ns celestial.api
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [metrics.ring.instrument :only  (instrument)]
        [taoensso.timbre :only (debug info error warn set-config! set-level!)]
        )
  (:require 
    [clj-yaml.core :as yaml ]
    [celestial.persistency :as p]
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [compojure.route :as route]))

(set-config! [:shared-appender-config :spit-filename ] "celestial.log")
(set-config! [:appenders :spit :enabled?] true)
(set-level! :trace)

(defn generate-response [data] {:status 200 :body data})

(defroutes provision-routes
  (POST "/:host" [host] 
        (let [machine (p/host host) type (p/type-of (:type machine))]
          (jobs/enqueue "provision" (merge machine type)) 
          (generate-response {:msg "submitted pupptization" :host host :machine machine :type type}))))

(defroutes stage-routes
  (POST "/:host" [host] 
        (jobs/enqueue "stage" (p/host host))
        (generate-response {:msg "submitted staging" :host host})))

(defroutes machine-routes
  (POST "/:host" [host]
        (jobs/enqueue "machine" (p/host host))
        (generate-response {:msg "submited system creation" :host host})))

(defroutes reg-routes
  (POST "/host" [& props]
        (p/register-host props)
        (generate-response {:msg "new host saved" :host (get-in props [:machine :hostname]) :props props}))
  (GET "/host/machine/:h" [h]
       (generate-response (p/host h)))
  (GET "/host/type/:h" [h]
       (generate-response (select-keys (p/type-of (:type (p/fuzzy-host h))) [:classes])))
  (POST "/type" [type & props]
        (p/new-type type props)
        (generate-response {:msg "new type saved" :type type :opts props}))
  ) 


(defroutes app-routes
  (context "/stage" [] stage-routes)
  (context "/provision" [] provision-routes)
  (context "/machine" [] machine-routes)
  (context "/registry" [] reg-routes)
  (route/not-found "Not Found"))

(def app
  (-> 
    (handler/api app-routes)
    (wrap-restful-params)
    (wrap-restful-response)
    (expose-metrics-as-json)
    (instrument)
    ))
