(ns es.migrations
 (:require 
  [re-core.common :refer (envs)]
  [puny.migrations :refer (Migration register)]
  [re-core.persistency.systems :as s]
  [es.common :refer (initialize)]
  [es.systems :refer (put)]
   
   ))

; indexing all systems
(defrecord ElasticSystems [identifier]
  Migration
  (apply- [this]
    (initialize)
    (doseq [id (flatten (map #(s/get-system-index :env (keyword %)) (envs)))]  
      (put id (s/get-system id))))  

  (rollback [this]))

(defn register-migrations []
  (register :systems-es (ElasticSystems. :systems-es-indexing)))
