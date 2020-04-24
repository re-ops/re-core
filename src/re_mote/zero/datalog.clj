(ns re-mote.zero.datalog
  "Re-mote datalog query on remote facts"
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.facts.datalog :refer (run-query)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol Datalog
  (query [this q]))

(extend-type Hosts
  Datalog
  (query [this q]
    [this (run-hosts this run-query [q])]))

(defn refer-osquery []
  (require '[re-mote.zero.datalog :as datalog :refer (query)]))
