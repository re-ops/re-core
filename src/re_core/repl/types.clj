(ns re-core.repl.types
  "Types repl functions"
  (:require
   [clansi.core :refer  (style)]
   [es.types :as t]
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
    [this {:types (t/all)}])

  (filter-by [this {:keys [types] :as m} f]
    [this {:types (filter f types)}])

  (add [this specs]
    (let [f (fn [{:keys [type] :as s}] (when (t/create s) [type s]))]
      [this {:types (map f specs)}]))

  (rm [this {:keys [types] :as m}]
    (doseq [[id _] types]
      (t/delete id))
    [this {:types []}]))

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
  (:puppet (t/get t)))

(defn refer-types []
  (require '[re-core.repl.types :as ts :refer (provision-type)]))

