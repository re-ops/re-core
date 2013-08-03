(ns celestial.routes.ui
 (:require 
   [celestial.routes.system :refer (system-routes)] 
   [celestial.routes.home :refer (home-routes)] 
   [celestial.routes.action :refer (action-routes)] 
   [compojure.core :refer (routes)] 
   [compojure.route :as route])
 )

(def ui-routes (routes action-routes system-routes home-routes))
