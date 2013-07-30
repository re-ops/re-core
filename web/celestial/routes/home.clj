(ns celestial.routes.home
  (:use compojure.core)
  (:require 
    [celestial.model :refer [figure-virt]]
    [celestial.models.system :as sys]
    [celestial.views.layout :as layout]
    [celestial.util :as util]
    [compojure.route :as route]))

(def base {:project-name "Celestial" :title "Celestial"})

(defn home-page []
  (layout/render "home.html" base))

(defn about-page []
  (layout/render "about.html"))

(defn rendered-system
   "transforms a system to rendered hash" 
   [[id {:keys [machine] :as system}]]
     (into (select-keys system [:type :description] ) 
       {:id id :hostname (machine :hostname) :hypervisor (name (figure-virt system))}))

(defn- systems 
  "renders systems table with given range" 
  [from to] 
   (layout/render "systems.html" 
      (into base {:systems (map rendered-system (sys/systems from to))})))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/view/systems" [from to] (systems (Integer/parseInt from) (Integer/parseInt to)))
  (GET "/about" [] (about-page))
  (route/files "/css/" {:root (str (System/getProperty "user.dir") "/public/css/")})
  (route/files "/js/" {:root (str (System/getProperty "user.dir") "/public/js/")}))
