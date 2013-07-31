(ns celestial.routes.home
  (:use compojure.core)
  (:require 
    [celestial.routes.common :refer [base]]
    [celestial.model :refer [figure-virt]]
    [celestial.models.system :as sys]
    [celestial.views.layout :as layout]
    [celestial.util :as util]
    [compojure.route :as route]))

(defn- home-page []
  (layout/render "home.html" base))

(defn- about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (route/files "/css/" {:root (str (System/getProperty "user.dir") "/public/css/")})
  (route/files "/js/" {:root (str (System/getProperty "user.dir") "/public/js/")}))
