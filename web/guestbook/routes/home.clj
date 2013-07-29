(ns guestbook.routes.home
  (:use compojure.core)
  (:require [guestbook.views.layout :as layout]
            [guestbook.util :as util]))

(defn home-page []
  (layout/render
    "home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page)))
