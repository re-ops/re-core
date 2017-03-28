(ns es.common
  "type mappings index etc.."
  (:require
    [re-core.common :refer (envs import-logging)]
    [es.node :as node :refer (ES)]
    [clojurewerkz.elastisch.native.index :as idx])
 )

(import-logging)

(def ^:const index "re-core-systems")

(def ^:const types {
  :jobs {
    :_ttl { :enabled true}
    :properties {
      :env {:type "string" :index "not_analyzed"}
      :status {:type "string"}
      :queue {:type "string"}
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
  [& [m & _]]
  (node/connect)
  (when-not (idx/exists? @ES index)
    (info "Creating index" index)
    (idx/create @ES index {:mappings types})))

(defn clear
  "Creates systems index and type"
  []
  (node/connect)
  (when (idx/exists? @ES index)
    (info "Clearing index" index)
    (idx/delete @ES index)))

(defn flush-
  []
  (when (idx/exists? @ES index)
    (trace "Flushing index" index)
    (idx/flush @ES index)))

(defn map-env-terms
   "maps should env terms to non-analysed forms"
   [query]
    (update-in query [:bool :should] (fn [ts] (into [] ts))))
