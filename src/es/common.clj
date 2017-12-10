(ns es.common
  "type mappings index etc.."
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [qbits.spandex :as s]
   [es.node :as node :refer (c)]
   [clojurewerkz.elastisch.native.index :as idx]))

(refer-timbre)

(def ^:const index "re-core")

(def ^:const types {:jobs {:properties {:env {:type "string" :index "not_analyzed"}
                                        :status {:type "string"}
                                        :queue {:type "string"}
                                        :start {:type "long"}
                                        :end {:type "long"}}}
                    :system {:properties {:owner {:type "string"}
                                          :env {:type "string" :index "not_analyzed"}
                                          :machine {:properties {:hostname {:type "string" :index "not_analyzed"}
                                                                 :cpus {:type "integer"}}}
                                          :type {:type "string"}}}})

(def ^:const settings {:number_of_shards 1})

(defn- exists?
  [index]
  (= (:status (s/request @c {:url [index] :method :head})) 200))

(defn- create
  [index mappings]
  (= (:status (s/request @c {:url [index] :method :put :body mappings})) 200))

(defn- delete
  [index]
  (= (:status (s/request @c {:url [index] :method :delete})) 200))

(defn initialize
  "Creates systems index and types"
  [& [m & _]]
  (node/connect)
  (when-not (exists? index)
    (info "Creating index" index)
    (create index {:mappings types})))

(defn clear
  "Creates systems index and type"
  []
  (node/connect)
  (when (exists? index)
    (info "Clearing index" index)
    (delete index)))
