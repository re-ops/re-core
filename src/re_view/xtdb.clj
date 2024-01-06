(ns re-view.xtdb
  (:require
   [xtdb.api :as xtdb]
   [mount.core :refer [defstate]]
   [re-share.config.core :refer [get!]]))

(defn connect
  "Connect to a xtdb database."
  [uri]
  (xtdb/new-api-client uri))

(defn into-db [c]
  (xtdb/db c))

(defstate client :start (connect (get! :shared :xtdb-uri)))

(defn query
  "Query a xtdb database."
  [query]
  (xtdb/q (into-db client) query))

(defn submit [tx]
  (xtdb/submit-tx client tx))

(defn entity [eid]
  (xtdb/entity (into-db client) eid))
