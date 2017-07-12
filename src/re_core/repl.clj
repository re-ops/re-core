(ns re-core.repl
  "Repl Driven re-core"
   (:require
     [clojure.core.strint :refer  (<<)]
     [re-mote.repl :refer (provision)]
     [re-core.repl.base :refer (refer-base)]
     [re-core.repl.systems :refer (refer-systems)]
     [re-core.repl.types :refer (refer-types)]
     [taoensso.timbre  :as timbre :refer (set-level!)])
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
  ([base t]
    (up (map (fn [i] (update-in base [:machine :hostname] (fn [n] (str n "-" i)))) (range t))))
  ([specs]
    (let [[_ {:keys [success]}] (run (add systems specs) | (sys/create) | (watch))]
     (println success)
      )))

(defn reload [f]
  (run (ls systems) | (filter-by f) | (sys/reload) | (watch)))

(defn clear [f]
  (run (ls systems) | (filter-by f) | (sys/clear) | (watch)))

(defn destroy
  ([]
   (destroy identity))
  ([f]
    (run (ls systems) | (filter-by f) | (ack) | (sys/destroy) | (watch))))

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
   (ssh (hosts))
   )
  ([{:keys [auth] :as hs}]
   (let [{:keys [user]} auth]
    (doseq [host (hs :hosts)]
      (.exec  (Runtime/getRuntime) (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~{user}@~{host}"))))) )


; Types
(defn all-types []
  (run (ls types ) | (pretty)))
