(ns celestial.routes.ui
 (:require 
   [celestial.routes.system :refer (system-routes)] 
   [celestial.routes.home :refer (home-routes)] 
   [compojure.core :refer (routes)] 
   [compojure.route :as route])
 )

(def ui-routes (routes system-routes home-routes))
