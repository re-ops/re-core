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

(def port-view 10)

(defn total-pages [c]
   (+ (int (/ c per-page)) (if (= (mod c per-page) 0) 0 1)))

(defn- pages 
  "Pagination pages" 
  [from total]
  {:pre [(> from -1)]}
  (let [to (min (+ from (* per-page port-view)) total)
        ps (partition 2 1 (range from to  per-page))
        pages (map (fn [[f t]] {:from f :to t :pos (quot f per-page)}) ps)]
     {:prev (if (> from 0) {:from (- from per-page) :to from :pos (quot (- from per-page) per-page)} nil)
      :items pages
      :next (if (< to total) {:from (- to per-page) :to to :pos (quot (- to per-page) per-page)} nil)}))

(defn- rendered-system
  "transforms a system to rendered hash" 
  [[id {:keys [machine] :as system}]]
  (into (select-keys system [:type :description] ) 
        {:id id :hostname (machine :hostname) :hypervisor (name (figure-virt system))}))

(defn- systems 
  "renders systems table with given range" 
  [from to] 
  (layout/render "systems.html" 
    (into base {:systems (map rendered-system (sys/systems from to)) :pages (pages from (sys/systems-count))})))

(defn- system
  "renders a system" 
  [id] 
  (let [system (sys/system id)]
    (case (figure-virt system)
      :proxmox  
        (layout/render "proxmox.html" (into base {:system (update-in system [:machine :os] name)}))
      :aws  (layout/render "aws.html" (into base {:system system}))
      )
    ))

(defroutes system-routes
  (GET "/view/systems" [from to] (systems (Integer/parseInt from) (Integer/parseInt to)))
  (GET "/view/system" [id] (system id)))
