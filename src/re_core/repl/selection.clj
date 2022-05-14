(ns re-core.repl.selection
  "comon selected hosts presets"
  (:require
   [re-core.repl :refer (hosts match-kv named hyp ip)]
   [re-mote.zero.management :refer (all-hosts)]))

(defn all-instances
  "All hosts which aren't marked as disabled"
  []
  (hosts (match-kv [:disabled] nil) :hostname))

(defn registered-instances
  "Hosts which have a hostname registered"
  []
  (hosts (named (keys (all-hosts))) :hostname))

(defn physical-registered-hosts
  "Physical hosts which are also registered"
  []
  (hosts
   (fn [[v m]]
     (and ((named (keys (all-hosts))) [v m]) ((hyp :physical) [v m]) (ip [v m]))) :hostname))
