(ns celestial.routes.action
 (:use compojure.core)
 (:require 
    [clojure.string :refer [join]]
    [celestial.model :refer [figure-rem]]
    [celestial.routes.common :refer [per-page base]]
    [celestial.models.action :as ac]
    [celestial.views.layout :as layout]
   [compojure.route :as route]))

(defn action-args [spec]
  (case (figure-rem spec)
    :capistrano  (->> spec vals first :args (join " "))
    )
  )

(defn renderable [{:keys [actions src]}]
  (map (fn [[a spec]] 
         (into {:name (name a) :type (name (figure-rem spec)) :args (action-args spec)} spec)) actions))

(defn action [type]
  (let [actions (flatten (map renderable (ac/actions-for-type type)))]
    (layout/render "actions.html" (into base {:type type :actions actions}))))

;; (flatten (map renderable (ac/actions-for-type "redis")))

(defroutes action-routes
  (GET "/view/actions" [type] (action type)))
