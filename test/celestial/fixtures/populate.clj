(ns celestial.fixtures.populate
  "data population"
  (:require
    [celestial.persistency :as p]  
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (admin ronen) :as d]))

(defn add-users 
  "populates admin and ronen users" 
  []
  (p/add-user admin)
  (p/add-user ronen))

(defn add-types 
   "populates types" 
   []
  (p/add-type d/redis-type))

(defn add-actions 
   "populates actions" 
   []
  (p/add-action d/redis-actions))

(defn add-systems []
  (doseq [i (range 100)] 
    (if (= 0 (mod i 2)) 
      (s/add-system d/redis-prox-spec)
      (s/add-system d/redis-ec2-spec))))


(defn populate-all 
   "populates all data types" 
   []
   (add-users)
   (add-types)
   (add-actions)
   (add-systems))
