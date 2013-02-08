(ns com.narkisr.celestial.api
  (:use compojure.core ring.middleware.edn
        [taoensso.timbre :only (debug info error warn)]
        [com.narkisr.celestial.tasks :only (reload puppetize)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (context "/provision" [] 
    (defroutes provision-routes
       (POST "/" [provision] 
         (debug "provisioning" provision)
         (puppetize provision)
         (generate-response {:status "pupptization done"}))))
  (context "/system" [] 
     (defroutes system-routes
       (POST "/:hypervisor" [hypervisor system]
         (debug "setting up system" system "on" hypervisor)
         (reload system hypervisor)
         (generate-response {:status "system is up"}))))

  (route/not-found "Not Found"))

(def app
  (-> (handler/api app-routes)
      (wrap-edn-params)))
