(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
   [re-share.time :refer (local-now to-long)]
   [com.rpl.specter :refer (transform ALL MAP-VALS multi-path)]
   [re-share.es.common :as es :refer (day-index get-es!)]
   [rubber.core :refer (create bulk-create)]
   [rubber.node :as node]
   [rubber.template :refer (template-exists? add-template)]
   [mount.core :as mount :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defstate elastic
  :start (node/connect (get-es!))
  :stop (node/stop))

(defprotocol Persistence
  (split [this m f])
  (mult [this] [this m])
  (enrich [this m t]))

(defprotocol Elasticsearch
  (persist
    [this m])
  (query
    [this q]))

(defn stamp [t]
  (fn [m]
    (merge m {:timestamp (or (-> m :result :timestamp) (to-long (local-now))) :type t})))

(defn by-hosts
  "split results by host"
  [{:keys [result] :as m}]
  (mapv (fn [[host r]] (assoc m :host (name host) :result r)) result))

(defn nested
  "split nested"
  [{:keys [result] :as m}]
  (mapv (fn [r] (assoc m :result r)) result))

(extend-type Hosts
  Persistence
  (enrich [this m t]
    (let [success-stamp (transform [:success ALL] (stamp t) m)
          failure-stamp (transform [:failure MAP-VALS ALL] (stamp (str t ".fail")) success-stamp)]
      [this failure-stamp]))

  (split [this {:keys [success] :as m} f]
    (let [splited (into [] (flatten (map f success)))
          hosts (distinct (map :host splited))]
      [this (assoc m :success splited :hosts hosts)]))

  Elasticsearch
  (persist
    ([this {:keys [success failure] :as m}]
     (when-not (empty? success)
       (bulk-create (day-index :re-mote :result) success))
     (let [fail (flatten (vals failure))]
       (when-not (empty? fail)
         (bulk-create (day-index :re-mote :result) fail)))
     [this m])))

(def types {:properties {:timestamp {:type "date" :format "epoch_millis"}
                         :host {:type "keyword"}
                         :type {:type "keyword"}}})

(defn initialize
  "setup Elasticsearch types and mappings for re-mote"
  []
  (es/initialize :re-mote {:result types} true)
  (when-not (template-exists? "re-mote-result")
    (add-template "re-mote-result" ["re-mote*"] {:number_of_shards 1} types)))

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist enrich split by-hosts nested)]))
