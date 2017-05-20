(ns re-core.repl
  "Repl Driven re-core"
   (:require
     [clojure.core.strint :refer  (<<)]
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

(defn create-from [base t]
  (let [specs (map (fn [i] (assoc-in base [:machine :hostname] (str "red" i))) (range t))]
    (run (add systems specs) | (create) | (watch) | (summary))))

(defn ip [[_ {:keys [machine] :as m}]] (machine :ip))

; by functions
(defn reload-by [f]
 (run (ls systems) | (filter-by f) | (reload) | (watch)))

(defn clear-by [f]
  (run (ls systems) | (filter-by f) | (clear) | (watch)))

(defn destroy-by [f]
   (run (ls systems) | (filter-by f) | (ack) | (destroy) | (watch)))

(defn into-hosts []
  (run (ls systems) | (filter-by ip) | (hosts)))

; greps
(defn stop-by-grep []
  (run (ls systems) | (grep :os :ubuntu-16.04) | (stop) | (watch)))

(defn list-by-grep []
  (run (ls systems) | (grep :os :ubuntu-16.04) | (pretty)))

; alls
(defn list-all []
  (run (ls systems) | (pretty)))

(defn clear-all []
   (run (ls systems) | (clear) | (watch)))

; utils
(defn ssh-into [{:keys [auth hosts]}]
  (let [{:keys [user]} auth]
    (doseq [host hosts]
      (.exec  (Runtime/getRuntime) (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~{user}@~{host}")))) )
