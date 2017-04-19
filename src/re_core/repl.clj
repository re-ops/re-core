(ns re-core.repl
  "Repl Driven re-core"
   (:require
     [re-mote.repl :refer :all]
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

(defn single [host]
  (run (ls systems) | (grep :hostname host)))

(defn create-from [base]
  (let [specs (map (fn [i] (assoc-in base [:machine :hostname] (str "red" i))) (range 2))]
    (run (add systems specs) | (create) | (watch))))

(defn ip [[_ {:keys [machine] :as m}]] (machine :ip))

; by functions
(defn reload-by [f]
 (run (ls systems) | (filter-by f) | (reload) | (watch)))

(defn clear-by [f]
  (run (ls systems) | (filter-by f) | (clear) | (watch)))

(defn destroy-by [f]
   (run (ls systems) | (filter-by f) |  (destroy) | (watch)))

(defn into-hosts []
  (run (ls systems) | (filter-by ip) | (hosts)))

; greps
(defn stop-by-grep []
  (run (ls systems) | (grep :os :ubuntu-15.04) | (stop) | (watch)))

(defn list-by-grep []
  (run (ls systems) | (grep :os :ubuntu-15.04) | (pretty)))

; alls
(defn list-all []
  (run (ls systems) | (pretty)))

(defn clear-all []
   (run (ls systems) | (clear) | (watch)))
