(ns celestial.fixtures.populate
  "data population"
  (:gen-class true)
  (:require
    [es.systems :as es]
    [celestial.security :refer (set-user)]
    [celestial.fixtures.core :refer (with-conf)]
    [simple-check.generators :as g]
    [celestial.redis :as red]  
    [celestial.persistency :as p]  
    [celestial.persistency.core :as c]  
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
  (p/add-type d/smokeping-type)
  (p/add-type d/redis-type))

(defn add-actions 
   "populates actions" 
   []
  (a/add-action d/redis-deploy)
  ; these actions won't work
  (a/add-action (assoc d/redis-deploy :name "restart-tomcat"))
  (a/add-action (assoc d/redis-deploy :name "flush-cache")))

(def machines 
  (g/fmap (partial zipmap [:hostname]) 
     (g/tuple 
       (g/fmap (partial apply str) 
          (g/tuple (g/elements ["zeus-" "atlas-" "romulus-" "remus-"]) g/nat)))))

(def host-env-gen
  (g/fmap (partial zipmap [:env :type])
      (g/tuple 
         (g/elements [:dev :qa :prod]) 
         (g/elements ["redis" "smokeping"]))))

(def systems-gen 
  (g/bind host-env-gen
    (fn [v] 
      (g/fmap #(merge % v) (g/elements [d/redis-prox-spec d/redis-ec2-spec])))))

(def systems-with-machines
  (g/bind machines
    (fn [v] 
      (g/fmap #(update-in % [:machine] (fn [m] (merge m v))) systems-gen))))

(defn add-systems []
  (doseq [s (g/sample systems-with-machines 100)] 
    (s/add-system s)))

(defn re-initlize
  "Re-init datastores"
  ([] (re-initlize false))
  ([clear-es]
   (c/initilize-puny)
   (when clear-es 
     (es/clear)
     (es/initialize))
   (red/clear-all)))

(defn populate-all 
  "populates all data types" 
  []
  (re-initlize true)
  (add-users)
  (add-types)
  (add-actions)
  (add-systems))

(defn populate-system 
  "Adds single type and system" 
  [t s]
  (re-initlize)
  (add-users)
  (p/add-type t)
  (s/add-system s))

(defn -main 
  "run populate all" 
  [& args]
  (set-user {:username "admin"}
     (populate-all)
     (p/delete-user "admin"); will be set up in next launch
     (println "populate done!")))
