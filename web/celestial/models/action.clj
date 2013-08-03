(ns celestial.models.action
  "Action model for the frontend"
  (:require 
    [slingshot.slingshot :refer [throw+]]
    [clojure.core.strint :refer  (<<)]
    [celestial.persistency :as p]))

(defn actions-for-type [type]
   (map #(p/get-action %) (p/get-action-index :operates-on type)))


;; (actions-for-type "redis")
