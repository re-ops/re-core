(ns celestial.api
  (:use compojure.core 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [metrics.ring.instrument :only  (instrument)]
        ring.middleware.edn
        ring.adapter.jetty 
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

(defroutes app-routes
  (context "/stage" []
           (defroutes stage-routes
             (POST "/" [system] 
                   (jobs/enqueue "stage" system)
                   (generate-response {:status "submitted staging"})))   
           )
  (context "/provision" [] 
           (defroutes provision-routes
             (POST "/" [provision] 
                   (jobs/enqueue "provision" provision)
                   (generate-response {:status "submitted pupptization"}))))
  (context "/machine" [] 
           (defroutes machine-routes
             (POST "/" [& spec]
                   (jobs/enqueue "machine" spec)
                   (generate-response {:status "submited system creation"}))))
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
