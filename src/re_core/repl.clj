(ns re-core.repl
  "Repl Driven re-core"
   (:require
     [re-mote.repl]
     [re-core.repl.base :refer (refer-base)]
     [re-core.repl.systems :refer (refer-systems)]
     [taoensso.timbre  :as timbre :refer (set-level!)])
   (:import
     [re_mote.repl.base Hosts]
     [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)

(set-level! :debug)

(def systems (Systems.))

(def types (Types.))

(defn stop-by-grep []
  (run (ls systems) | (grep :os :ubuntu-15.04) | (stop) | (watch)))

(defn list-by-grep []
  (run (ls systems) | (grep :os :ubuntu-15.04) | (pretty)))

(defn single [host]
  (run (ls systems) | (grep :hostname host)))

(defn reload-by [f]
 (run (f systems) | (reload) | (watch)))

(defn create-from [base]
  (let [specs (map (fn [i] (assoc-in base [:machine :hostname] (str "red" i))) (range 2))]
    (run (add systems specs) | (create) | (watch))))

(defn destroy-all []
   (run (ls systems) | (destroy) | (watch)))

(defn clear-all []
   (run (ls systems) | (clear) | (watch)))

(defn update-all [ssh]
  (run (ls systems) | (hosts ssh))
  )

