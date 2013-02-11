(ns celestial.api
  (:use compojure.core 
        [metrics.ring.expose :only  (expose-metrics-as-json)]
        [metrics.ring.instrument :only  (instrument)]
        ring.middleware.edn
        ring.adapter.jetty 
        [taoensso.timbre :only (debug info error warn set-config!)]
        [celestial.jobs :only (enqueue)])
  (:require 
    [compojure.handler :as handler]
    [compojure.route :as route]))

(set-config! [:shared-appender-config :spit-filename ] "/home/ronen/code/celestial/celestial.log")
(set-config! [:appenders :spit :enabled?] true)

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (context "/provision" [] 
           (defroutes provision-routes
             (POST "/" [provision] 
                   (enqueue "provision" provision)
                   (generate-response {:status "submitted pupptization"}))))
  (context "/system" [] 
           (defroutes system-routes
             (POST "/:hypervisor" [hypervisor system]
                   (enqueue "system" {:hypervisor hypervisor :system system})
                   (generate-response {:status "system is up"}))))
  (route/not-found "Not Found"))

(def app
  (-> (handler/api app-routes)
    (expose-metrics-as-json)
    (instrument)
    (wrap-edn-params)))


(defn -main []
  (run-jetty app  {:port 8080 :join? false}))
