(ns re-core.repl.selection
  "comon selected hosts presets"
  (:require
   [re-core.repl :refer (hosts match-kv named)]
   [re-mote.zero.management :refer (all-hosts)]))

(defn all-instances []
  (hosts (match-kv [:disabled] nil) :hostname))

(defn registered-instances []
  (hosts (named (keys (all-hosts))) :hostname))
