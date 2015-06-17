(ns celestial.fixtures.populate
  "data population"
  (:gen-class true)
  (:require
    [celestial.persistency [users :as u] [types :as t]]  
    [es.common :as es]
    [celestial.model :refer (figure-virt)]
    [celestial.security :refer (set-user)]
    [celestial.fixtures.core :refer (with-conf)]
    [clojure.test.check.generators :as g]
    [celestial.redis :as red]  
    [celestial.persistency.core :as c]  
    [celestial.persistency.systems :as s]
    [celestial.persistency.actions :as a]
    [celestial.fixtures.data :refer (admin ronen) :as d]))

(defn add-users 
  "populates admin and ronen users" 
  []
  (u/add-user admin)
  (u/add-user ronen))

(defn add-types 
   "populates types" 
   []
  (t/add-type d/smokeping-type)
  (t/add-type d/redis-type))

(defn add-actions 
   "populates actions" 
   []
  (a/add-action d/redis-deploy)
  ; these actions won't work
  (a/add-action (assoc d/redis-deploy :name "restart-tomcat"))
  (a/add-action (assoc d/redis-deploy :name "flush-cache")))

(def host 
  (g/fmap (partial apply str) 
    (g/tuple (g/elements ["zeus-" "atlas-" "romulus-" "remus-"]) g/nat)))

(def ip (g/fmap #(str "192.168.1." %) (g/such-that #(<= (.length %) 3) (g/fmap str g/nat))))

(def machines 
  (g/fmap (partial zipmap [:hostname :ip]) (g/tuple host ip)))

(def host-env-gen
  (g/fmap (partial zipmap [:env :type])
      (g/tuple 
         (g/elements [:dev :qa :prod]) 
         (g/elements ["redis" "smokeping"]))))

(defn with-ids [m]
  (let [virt (figure-virt m) ids {:proxmox :vmid :openstack :instance-id :aws :instance-id}]
    (g/fmap #(merge-with merge m %) 
      (g/hash-map virt 
        (g/hash-map (ids virt) (g/such-that #(> (.length %) 10) g/string-alphanumeric 100))))))

(def systems-gen 
  (g/bind host-env-gen
    (fn [v] 
      (g/fmap #(merge % v) 
        (g/one-of 
          (into [(g/return d/redis-prox-spec)] (mapv with-ids [d/redis-ec2-spec d/redis-openstack-spec])))))))

(def instance-keys {:openstack [:openstack :instance-id] [:aws :instance-id] [:proxmox :id]})


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
   (when clear-es (es/clear))
   (es/initialize)
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
  (t/add-type t)
  (s/add-system s))

(defn -main 
  "run populate all" 
  [& args]
  (set-user {:username "admin"}
     (populate-all)
     (u/delete-user "admin"); will be set up in next launch
     (println "populate done!")))
