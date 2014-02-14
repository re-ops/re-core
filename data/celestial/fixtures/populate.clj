(ns celestial.fixtures.populate
  "data population"
  (:gen-class true)
  (:require
    [celestial.security :refer (set-user)]
    [celestial.fixtures.core :refer (with-conf)]
    [simple-check.generators :as g]
    [celestial.redis :refer (clear-all)]  
    [celestial.persistency :as p]  
    [celestial.persistency.systems :as s]
    [celestial.persistency.actions :as a]
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
  (a/add-action d/redis-deploy))

(def env-gen (g/elements [:dev :qa :prod])) 

(def systems-gen 
  (g/bind env-gen 
    (fn [v] 
      (g/fmap #(assoc % :env v) (g/elements [d/redis-prox-spec d/redis-ec2-spec])))))

(defn add-systems []
  (doseq [s (g/sample systems-gen 100)] 
    (s/add-system s)))

(defn populate-all 
   "populates all data types" 
   []
   (clear-all)
   (add-users)
   (add-types)
   (add-actions)
   (add-systems))

(defn populate-system 
   "Adds single type and system" 
   [t s]
  (clear-all)
  (add-users)
  (p/add-type t)
  (s/add-system s))

(defn -main 
   "run populate all" 
   [& args]
  (with-conf
   (set-user {:username "admin"}
     (populate-all)
     (p/delete-user "admin")
     (println "populate done!")        )))
