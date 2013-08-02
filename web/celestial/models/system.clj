(ns celestial.models.system
  "System model for the frontend"
  (:require 
    [slingshot.slingshot :refer [throw+]]
    [clojure.core.strint :refer  (<<)]
    [celestial.persistency :as p]))

(defn systems 
  "system list range" 
  [from to]
  {:pre [(> from -1)]}
  (let [systems (into [] (p/all-systems))]
    (if (and (contains? systems from) (contains? systems to))
      (map (juxt identity p/get-system) (subvec systems from to)) 
      (throw+ {:type ::non-legal-range :message (<<  "No legal systems in range ~{from}:~{to} try between ~{0}:~(count systems)")}))))

(defn systems-count 
  "How many systems there are"
  []
  (count (p/all-systems)))


(defn system [id] (p/get-system id))
