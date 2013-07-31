(ns celestial.routes.system
  "System routes"
 (:use compojure.core)
 (:require 
    [celestial.routes.common :refer [per-page base]]
    [celestial.model :refer [figure-virt]]
    [celestial.models.system :as sys]
    [celestial.views.layout :as layout]
    [celestial.util :as util]
    [compojure.route :as route]))


(defn- pages 
   "pagination for systems" 
   []
   (map #(hash-map :from (* per-page %) :to (*  per-page (+ % 1)) :pos %) (range (int (/ (sys/systems-count) per-page))))
  )

(defn- rendered-system
  "transforms a system to rendered hash" 
  [[id {:keys [machine] :as system}]]
  (into (select-keys system [:type :description] ) 
        {:id id :hostname (machine :hostname) :hypervisor (name (figure-virt system))}))

(defn- systems 
  "renders systems table with given range" 
  [from to] 
  (layout/render "systems.html" 
    (into base {:systems (map rendered-system (sys/systems from to)) :pages (pages)})))
 
(defroutes system-routes
  (GET "/view/systems" [from to] (systems (Integer/parseInt from) (Integer/parseInt to))))
