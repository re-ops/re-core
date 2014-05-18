(ns es.common
  "type mappings index etc.."
  (:require 
    [celestial.common :refer (envs import-logging)]
    [es.node :as node]
    [clojurewerkz.elastisch.native.index :as idx])
 )

(import-logging)

(def ^:const index "celestial-systems")

(def ^:const types {
  :jobs {
    :properties {
      :env {:type "string" :index "not_analyzed"}
      :status {:type "string" :index "not_analyzed"}
      :start {:type "long"}
      :end {:type "long"}
     }
  }
   
  :system {
    :properties {
      :owner {:type "string" }
      :env {:type "string" :index "not_analyzed"}
      :machine {
        :properties {
          :hostname {:type "string" :index "not_analyzed"}
          :cpus {:type "integer"}
        }
      }
      :type {:type "string"}
     }
    }
   })

(defn initialize 
  "Creates systems index and types" 
  []
  (node/start-n-connect [index])
  (when-not (idx/exists? index)
    (info "Creating index" index)
    (idx/create index :mappings types)))

(defn clear 
  "Creates systems index and type" 
  []
  (node/start-n-connect [index])
  (when (idx/exists? index)
    (info "Clearing index" index)
    (idx/delete index)))

(defn flush- 
  []
  (when (idx/exists? index)
    (info "Flushing index" index)
    (idx/flush index)))
 
(defn env-term 
   "map a single term env into a non analysed form" 
   [{:keys [term] :as m}]
    (if (:env term) 
      (update-in m [:term :env] #(str ":" %)) m))

(defn map-env-terms 
   "maps should env terms to non-analysed forms" 
   [query]
  (update-in query [:bool :should] (fn [ts] (mapv env-term ts))))
