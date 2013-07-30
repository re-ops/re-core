(ns celestial.handler  
  (:require [compojure.core :refer [defroutes]]            
            [celestial.routes.home :refer [home-routes]]
            [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            ))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn destroy []
  (timbre/info "picture-gallery is shutting down"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "celestial is shutting down..."))

(def app (middleware/app-handler
           ;;add your application routes here
           [home-routes app-routes]
           ;;add custom middleware here           
           :middleware []
           ;;add access rules here
           ;;each rule should be a vector
           :access-rules []))

