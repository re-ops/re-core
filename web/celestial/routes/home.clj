(ns celestial.routes.home
  (:use compojure.core)
  (:require [celestial.views.layout :as layout]
            [celestial.util :as util]
            [compojure.route :as route]))

(def base {:project-name "Celestial" :title "Celestial"})

(defn home-page []
  (layout/render "home.html" base))

(defn about-page []
  (layout/render "about.html"))

(defn systems 
   "renders systems table with given range" 
   [from to]
   (layout/render "systems.html" base))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/view/systems" [from to] (systems from to))
  (GET "/about" [] (about-page))
  (route/files "/css/" {:root (str (System/getProperty "user.dir") "/public/css/")})
  (route/files "/js/" {:root (str (System/getProperty "user.dir") "/public/js/")}))
