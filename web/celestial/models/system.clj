(ns celestial.models.system
  "System model for the frontend"
  (:require 
    [slingshot.slingshot :refer [throw+]]
    [clojure.core.strint :refer  (<<)]
    [celestial.persistency :as p]))

(defn systems 
  "Get systems in range by type" 
  [from to type]
  {:pre [(> from -1) (if type (p/type-exists! type) true)]}
  (let [systems (if type (p/get-system-index :type type) (into [] (p/all-systems))) to* (min to  (count systems))]
    (when-not (empty? systems)
      (if (and (contains? systems from) (contains? systems to*))
        (map (juxt identity p/get-system) (subvec systems from to*)) 
        (throw+ {:type ::non-legal-range :message (<<  "No legal systems in range ~{from}:~{to*} try between ~{0}:~(count systems)")})))))

(defn systems-count 
  "Count systems by type"
  [type] 
   (if type
     (count (p/get-system-index :type type))
     (count (p/all-systems))
     ))


(defn system [id] (p/get-system id))
