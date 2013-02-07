(ns com.narkisr.celestial.api
  (:use compojure.core ring.middleware.edn
        [taoensso.timbre :only (debug info error warn)]
        [com.narkisr.celestial.tasks :only (reload puppetize)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (context "/provision" [] 
           (defroutes provision-routes
             (POST "/" {body :body} 
                   (let [{:keys [provision]} body] (puppetize provision)))))
  (context "/system" [] 
           (defroutes system-routes
             (POST "/:hypervisor" [hypervisor system]
                  (debug "setting up system" system "on" hypervisor)
                  (reload system hypervisor))))
  (route/not-found "Not Found"))

(def app
  (-> (handler/api app-routes)
      (wrap-edn-params)))
