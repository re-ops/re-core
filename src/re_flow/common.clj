(ns re-flow.common
  "common flow functions"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.repl :refer (named) :as repl]
   [com.rpl.specter :refer [select ALL keypath]]))

(refer-timbre)

(defn successful-systems
  "Get the successful system ids from a Re-core pipeline result"
  [f]
  (select [ALL (keypath :results :success) ALL :args ALL :system-id] @f))

(defn successful-hosts
  "Get the successful hots ids from a Re-mote pipeline result"
  [hs]
  (select [ALL (keypath :success) ALL :host] hs))

(defn successful-ids [hs]
  (let [systems (repl/list (named (successful-hosts hs)) :systems :print? false)]
    (into #{} (->> systems second :systems (map first)))))
