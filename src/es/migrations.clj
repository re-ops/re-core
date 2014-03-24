(ns es.migrations
 (:require 
  [celestial.common :refer (envs)]
  [puny.migrations :refer (Migration register)]
  [celestial.persistency.systems :as s]
  [es.systems :as es]
   )  
 )

; indexing all systems
(defrecord ElasticSystems [identifier]
  Migration
  (apply- [this]
    (es/initialize)
    (doseq [id (flatten (map #(s/get-system-index :env (keyword %)) (envs)))]  
      (es/put id (s/get-system id))))  

  (rollback [this]))

(defn register-migrations []
  (register :systems-es (ElasticSystems. :systems-es-indexing)))
