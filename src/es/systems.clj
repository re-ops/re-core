(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get])
  (:require
   [es.common :refer (flush- index map-env-terms clear initialize)]
   [es.node :as node :refer (ES)]
   [clojure.set :refer (subset?)]
   [clojure.core.strint :refer (<<)]
   [slingshot.slingshot :refer  [throw+]]
   [re-core.common :refer (envs)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.native.document :as doc]))

(refer-timbre)

(def ^:dynamic flush? false)

(defmacro set-flush
  [flush* & body]
  `(binding [flush? ~flush*] ~@body))

(defn put
  "Add/Update a system into ES"
  [id system]
  (doc/put @ES index "system" id system)
  (when flush? (flush-)))

(defn delete
  "delete a system from ES"
  [id & {:keys [flush?]}]
  (doc/delete @ES index "system" id)
  (when flush? (flush-)))

(defn get
  "Grabs a system by an id"
  [id]
  (doc/get @ES index "system" id))

(defn query
  "basic query string"
  [query & {:keys [from size] :or {size 100 from 0}}]
  (doc/search @ES index "system" {:from from :size size :query query :fields ["owner" "env"]}))

