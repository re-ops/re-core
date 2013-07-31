(ns celestial.models.system
  "System model for the frontend"
  (:require [celestial.persistency :as p]))

(defn systems 
   "system list range" 
   [from to]
   (map (juxt identity p/get-system ) (subvec (into [] (p/all-systems)) from to)))

(defn systems-count 
  "How many systems there are"
   []
  (count (p/all-systems)))

