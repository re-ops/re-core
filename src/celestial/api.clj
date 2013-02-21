(ns celestial.api
  (:gen-class true)
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [ring.middleware.format-params :only [wrap-restful-params]]
        [ring.middleware.format-response :only [wrap-restful-response]]
        [metrics.ring.instrument :only  (instrument)]
        [ring.middleware.edn :only (wrap-edn-params)]
        [ring.adapter.jetty :only (run-jetty)] 
        [taoensso.timbre :only (debug info error warn set-config!)]
        [celestial.jobs :only (enqueue initialize-workers clear-all)])
  (:require 
    [clj-yaml.core :as yaml ]
    [celestial.persistency :as p]
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [compojure.route :as route]))

(set-config! [:shared-appender-config :spit-filename ] "celestial.log")
(set-config! [:appenders :spit :enabled?] true)

(defn generate-response [data] {:status 200 :body data})

(defroutes provision-routes
  (POST "/:host" [host] 
        (let [machine (p/host host) type (p/type (:type machine))]
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
  (POST "/host" [machine type]
        (let [{:keys [hostname]} machine]
          (p/register-host hostname type machine) 
          (generate-response {:msg "new host saved" :host hostname :machine machine :type type})))
  (GET "/host/machine/:h" [h]
       (generate-response (p/host h)))
  (GET "/host/type/:h" [h]
       (debug (select-keys (p/type (:type (p/fuzzy-host h))) [:classes]))
       (generate-response (select-keys (p/type (:type (p/fuzzy-host h))) [:classes])))
  (POST "/type" [type module classes]
        (p/new-type type {:module module :classes classes})
        (generate-response {:msg "new type saved" :type type :classes classes}))
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


(defn add-shutdown []
  (.addShutdownHook (Runtime/getRuntime) 
                    (Thread. 
                      (fn [] 
                        (debug "Shutting down...")
                        (jobs/shutdown-workers)))) )

(defn -main [& args]
  (add-shutdown)
  (jobs/clear-all)
  (jobs/initialize-workers)
  (run-jetty app  {:port 8080 :join? true}))
