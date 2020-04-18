(ns es.history
  "lein history persistence"
  (:refer-clojure :exclude [get partial])
  (:require
   [re-core.common :refer (hostname)]
   [qbits.spandex :as s]
   [es.common :refer (index)]
   [rubber.node :refer (connection)]
   [rubber.core :as z]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.model :as model]))

(refer-timbre)

(def path ".lein-repl-history")

(defn history-m []
  {:history (slurp path) :date (System/currentTimeMillis)})

(defn create
  "Add history for the current host"
  ([hist]
   (try
     (= (:status (s/request (connection) {:url [(index :history) hostname] :method :post :body hist})) 200)
     (catch Exception e
       (error e
              (ex-data e)
              (throw e))))))
(defn put
  "Update history"
  [hist]
  (z/put (index :history) hostname hist))

(defn delete
  "delete history"
  [t]
  (z/delete (index :history) hostname))

(defn exists?
  [host]
  (z/exists? (index :history) :history host))

(defn get
  "Grabs history for hostname"
  [host]
  (z/get (index :history) host))

(defn restore []
  (let [{:keys [history]} (get hostname)]
    (spit path history)))

(defn save []
  (if (exists? hostname)
    (put hostname (history-m))
    (create (history-m))))

(defn refer-history []
  (require '[es.history :as hist]))
