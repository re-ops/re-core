(ns re-core.persistency.xtdb
  (:require
   [clojure.java.io :as io]
   [mount.core :as mount :refer (defstate)]
   [xtdb.api :as xt]))

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file dir)
                        :sync? true}})]
    (xt/start-node
     {:xtdb/tx-log (kv-store "data/dev/tx-log")
      :xtdb/document-store (kv-store "data/dev/doc-store")
      :xtdb/index-store (kv-store "data/dev/index-store")})))

(defn stop-xtdb! [node]
  (.close node))

(defstate node
  :start (start-xtdb!)
  :stop (stop-xtdb! node))
