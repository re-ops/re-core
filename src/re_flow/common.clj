(ns re-flow.common
  "common flow functions"
  (:require
   [com.rpl.specter :refer [select ALL keypath]]))

(defn successful-systems
  "Get the successful system ids from a Re-core pipeline result"
  [f]
  (select [ALL (keypath :results :success) ALL :args ALL :system-id] @f))

(defn successful-hosts
  "Get the successful hots ids from a Re-mote pipeline result"
  [hs]
  (select [ALL (keypath :success) ALL :host] hs))
