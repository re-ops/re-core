(ns celestial.models.system
  "System model for the frontend"
  (:require [celestial.persistency :as p])
 )

(defn systems 
   "system list range" 
   [from to]
   (map (juxt identity p/get-system ) (take to (drop from (p/all-systems)))))
