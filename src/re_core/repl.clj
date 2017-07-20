(ns re-core.repl
  "Repl Driven re-core"
   (:refer-clojure :exclude [list update])
   (:require
     [clojure.core.strint :refer  (<<)]
     [re-mote.repl :refer (provision)]
     [re-core.repl.base :refer (refer-base)]
     [re-core.repl.systems :refer (refer-systems)]
     [re-core.repl.types :refer (refer-types)]
     [taoensso.timbre :as timbre :refer (set-level!)])
   (:import
     [re_mote.repl.base Hosts]
     [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)
(refer-types)

(set-level! :debug)

(def systems (Systems.))
(def types (Types.))

(defn single [host]
  (run (ls systems) | (grep :hostname host)))

; filtering

(defn typed
   "get instances by type"
   [fixture]
   (fn [{:keys [type]}] (=  (fixture :type))))

(defn ip
  "machine has an ip"
  [[_ {:keys [machine] :as m}]]
  (machine :ip))

; management

(defn up
  "Create VM and provision:
    (up kvm-instance 5) ; create 5 VM instances
    (up kvm-instance); create a single VM "
  ([base t]
    (let [specs (map (fn [i] (update-in base [:machine :hostname] (fn [n] (str n "-" i)))) (range t))
          [_ m] (run (add systems specs) | (sys/create) | (wait-on) | (pretty-print))
          by-type (group-by (fn [s] (get-in s [1 :type])) (:systems m))]
      (doseq [[t ms] by-type]
         (provision (into-hosts systems {:systems ms}) (source types t) (<< "/tmp/~{t}"))
        )))
  ([single]
    (up single 1)))

(defn reload [f]
  (run (ls systems) | (filter-by f) | (sys/reload) | (watch)))

(defn clear [f]
  (run (ls systems) | (filter-by f) | (sys/clear) | (watch)))

(defn destroy
  ([]
   (destroy identity {}))
  ([opts]
   (destroy identity opts))
  ([f opts]
    (run (ls systems) | (filter-by f) | (ack opts) | (sys/destroy) | (wait-on) | (pretty-print))))

(defn halt
  ([]
    (halt identity))
  ([f]
    (run (ls systems) | (filter-by f) | (sys/stop) | (watch))))

(defn start
  ([]
   (start identity))
  ([f]
    (run (ls systems) | (filter-by f) | (sys/start) | (watch))))

(defn list
  ([]
    (list identity))
  ([f]
    (run (ls systems) | (filter-by f) | (pretty))))

(defn hosts
  ([]
   (hosts ip))
  ([f]
    (run (ls systems) | (filter-by f) | (into-hosts))))

(defn ssh
  ([]
   (ssh (hosts)))
  ([{:keys [auth] :as hs}]
   (let [{:keys [user]} auth]
    (doseq [host (hs :hosts)]
      (.exec  (Runtime/getRuntime) (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~{user}@~{host}"))))) )


; Types
(defn all-types []
  (run (ls types ) | (pretty)))
