(ns es.common
  "type mappings index etc.."
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [qbits.spandex :as s]
   [es.node :as node :refer (c)]))

(refer-timbre)

(def ^:const index "re-core")

(def ^:const types {:jobs {:properties {:env {:type "keyword"}
                                        :status {:type "text"}
                                        :queue {:type "text"}
                                        :start {:type "long"}
                                        :end {:type "long"}}}
                    :system {:properties {:owner {:type "text"}
                                          :env {:type "keyword"}
                                          :machine {:properties {:hostname {:type "keyword" :index "not_analyzed"}
                                                                 :cpus {:type "integer"}}}
                                          :type {:type "keyword"}}}})

(def ^:const settings {:number_of_shards 1})

(defn- exists?
  [index]
  (try
    (= (:status (s/request @c {:url [index] :method :head})) 200)
    (catch Exception e
      (info (ex-data e))
      false)))

(defn- create
  [index mappings]
  (= (:status (s/request @c {:url [index] :method :put :body mappings})) 200))

(defn- delete
  [index]
  (= (:status (s/request @c {:url [index] :method :delete})) 200))

(defn initialize
  "Creates systems index and types"
  [& [m & _]]
  (when-not (exists? index)
    (info "Creating index" index)
    (create index {:mappings types})))

(defn clear
  "Creates systems index and type"
  []
  (when (exists? index)
    (info "Clearing index" index)
    (delete index)))
