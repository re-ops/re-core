(ns re-mote.zero.osquery
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.facts.osquery :refer (osquery)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol OsQuery
  (query [this q]))

(extend-type Hosts
  OsQuery
  (query [this q]
    [this (run-hosts this osquery [q])]))

(defn refer-osquery []
  (require '[re-mote.zero.osquery :as osquery :refer (query)]))
