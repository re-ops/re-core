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
             (POST "/:host" [host] 
                   (jobs/enqueue "provision" (p/host host))
                   (generate-response {:status "submitted pupptization" :host host})))

(defroutes stage-routes
             (POST "/:host" [host] 
                   (jobs/enqueue "stage" (p/host host))
                   (generate-response {:status "submitted staging" :host host})))

(defroutes machine-routes
             (POST "/:host" [host]
                   (jobs/enqueue "machine" (p/host host))
                   (generate-response {:status "submited system creation" :host host})))

(defroutes reg-routes
  (POST "/host" [machine type]
        (let [{:keys [hostname]} machine]
          (p/register-host hostname type machine) 
          (generate-response {:status "new host saved" :host hostname :machine machine :type type})))
  (GET "/host/:h" [h]
       (generate-response (:type (p/host h))))
  (POST "/type" [type sandbox classes]
        (p/new-type type classes)
        (generate-response {:status "new type saved" :type type :classes classes})))

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
