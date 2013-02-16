(ns celestial.api
  (:use [compojure.core :only (defroutes context POST GET)] 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [metrics.ring.instrument :only  (instrument)]
        [ring.middleware.edn :only (wrap-edn-params)]
        [ring.adapter.jetty :only (run-jetty)] 
        [taoensso.timbre :only (debug info error warn set-config!)]
        [celestial.jobs :only (enqueue initialize-workers clear-all)])
  (:require 
    [celestial.jobs :as jobs]
    [compojure.handler :as handler]
    [compojure.route :as route]))

(set-config! [:shared-appender-config :spit-filename ] "/home/ronen/code/celestial/celestial.log")
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

(defroutes app-routes
  (context "/stage" [] stage-routes)
  (context "/provision" [] provision-routes)
  (context "/machine" [] machine-routes)
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

(defn -main []
  (add-shutdown)
  (jobs/clear-all)
  (jobs/initialize-workers)
  (run-jetty app  {:port 8080 :join? true}))
