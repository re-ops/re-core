(ns re-core.repl.types
  "Types repl functions"
  (:require
   [clansi.core :refer  (style)]
   [re-core.persistency.types :as t]
   [re-core.repl.base :refer [Repl Report]])
  (:import
   [re_core.repl.base Types]))

(defprotocol Source
  (source [this t]))

(extend-type Types
  Repl
  (ls [this & opts]
    (ls this))
  (ls [this]
    [this {:types (map t/get-type (t/all-types))}])
  (filter-by [this {:keys [types] :as m} f]
    [this {:types (filter f types)}]))

(extend-type Types
  Report
  (summary [this ts]
    (println "")
    (println (style "Types summary:" :blue) "\n")
    (doseq [t ts]
      (println " " t))
    (println "")
    [this ts]))

(defn provision-type [t]
  (:puppet (t/get-type t)))

(defn refer-types []
  (require '[re-core.repl.types :as ts :refer (provision-type)]))
