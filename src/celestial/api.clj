(ns celestial.api
  (:gen-class true)
  (:refer-clojure :exclude [type])
  (:use [compojure.core :only (defroutes context POST GET)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [metrics.ring.instrument :only  (instrument)]
        [ring.middleware.edn :only (wrap-edn-params)]
        [clj-yaml.core :only (generate-string parse-string)]
        [ring.adapter.jetty :only (run-jetty)] 
        [taoensso.timbre :only (debug info error warn set-config!)]
        [celestial.jobs :only (enqueue initialize-workers clear-all)])
  (:require 
    [celestial.persistency :as p]
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [compojure.route :as route]))

(set-config! [:shared-appender-config :spit-filename ] "celestial.log")
(set-config! [:appenders :spit :enabled?] true)

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes provision-routes
             (POST "/" [provision] 
                   (jobs/enqueue "provision" provision)
                   (generate-response {:status "submitted pupptization"})))

(defroutes stage-routes
             (POST "/" [system] 
                   (jobs/enqueue "stage" system)
                   (generate-response {:status "submitted staging"})))

(defroutes machine-routes
             (POST "/" [& spec]
                   (jobs/enqueue "machine" spec)
                   (generate-response {:status "submited system creation"})))

(defroutes reg-routes
  (POST "/host" [machine type host]
        (p/register-host host type machine)
        (generate-response {:status "new host saved" :host host :machine machine :type type}))
  (GET "/host/:h" [h]
       (generate-string (:type (p/host h))))
  (POST "/type/:t" [t & spec]
        (p/new-type t spec)
        (generate-response {:status "new type saved" :type t :spec spec})))

(defroutes app-routes
  (context "/stage" [] stage-routes)
  (context "/provision" [] provision-routes)
  (context "/machine" [] machine-routes)
  (context "/registry" [] reg-routes)
  (route/not-found "Not Found"))

(def app
  (-> (handler/api app-routes)
    (expose-metrics-as-json)
    (instrument)
    (wrap-edn-params)))


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
