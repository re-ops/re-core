(ns re-core.repl
  "Repl Driven re-core"
  (:refer-clojure :exclude [list update])
  (:require
   [clojure.core.strint :refer  (<<)]
   [re-mote.repl :as mote]
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

; filtering functions

(defn by-type
  "get instances by type"
  [t]
  (fn [[_ {:keys [type]}]] (=  type t)))

(defn ip
  "machine has an ip (usually means its running)"
  [[_ {:keys [machine] :as m}]]
  (machine :ip))

(defn with-ids
  "instances by id"
  [ids]
  (fn [[id _]]
    ((into #{} (map str ids)) (str id))))

; management

(defn reload
  "Reload (stop destroy and up):
   (reload) ; reload all running instances
   (reload (by-type :redis)) ; reload all redis instances
  "
  ([]
   (reload ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/reload) | (block-wait) | (pretty-print "reload"))))

(defn clear
  " Clear model only (VM won't be deleted):
    (clear) ; clear all systems (both runnging and non running)
    (clear (by-type :redis)) ; clear systems with redis type
    (clear identity :types) ; clear all types
  "
  ([]
   (clear identity))
  ([f]
   (clear f :systems))
  ([f on]
   (case on
     :systems (run (ls systems) | (filter-by f) | (rm) | (pretty))
     :types (run (ls types) | (filter-by f) | (rm) | (pretty)))))

(defn destroy
  " Destroy instances (both clear and remove VM):
     (destroy) ; remove all instances (both running and non running)
     (destroy ip) ; remove running instances only
     (destroy ip {:force true}) ; remove running instances only without confirmation
  "
  ([]
   (destroy identity {}))
  ([f]
   (destroy f {}))
  ([f opts]
   (run (ls systems) | (filter-by f) | (ack opts) | (sys/destroy) | (async-wait pretty-print "destroy"))))

(defn halt
  " Halt instances:
   (halt) ; halt all running (have ip)
   (halt (single \"foo\")) ; halt host foo
  "
  ([]
   (halt ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/stop) | (async-wait pretty-print "halt"))))

(defn start
  "Start instances:
    (start) ; start all without ip (stopped)
    (start (by-type :redis)) ; start all redis types
  "
  ([]
   (start (comp not ip)))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/start) | (block-wait) | (pretty-print "start"))))

(defn list
  "List available instances:
    (list) ; list all systems
    (list ip) ; list all systems that have an ip (running)
    (list identity :types) ; list all types
  "
  ([]
   (list identity :systems))
  ([f]
   (list f :systems))
  ([f on]
   (case on
     :systems (run (ls systems) | (filter-by f) | (pretty))
     :types (run (ls types) | (pretty)))))

(defn hosts
  "Convert systems into re-mote hosts:
    (hosts) ; all systems using ip address
    (hosts (by-type :redis) :hostname) ; all redis instances using hostname
  "
  ([]
   (hosts ip :ip))
  ([f k]
   (run (ls systems) | (filter-by f) | (into-hosts k))))

(defn provision
  "Provision VM:
    (provision) ; run provision on all running instances
    (provision (fn [{:keys [type]] (= type :redis))) ; provision using filter fn"
  ([]
   (provision ip))
  ([f]
   (let [[_ m] (run (ls systems) | (filter-by f))
         by-type (group-by (comp :type second) (:systems m))]
     (doseq [[t ms] by-type]
       (mote/provision (into-hosts systems {:systems ms} :ip) (provision-type t))))))

(defn up
  "Create VM and provision:
    (up (kvm-instance \"repos-kvm\")  5) ; create 5 VM instances
    (up (kvm-instance \"repos-kvm\")); create a single VM
    (up (kvm-instance \"repos-kvm\") 1 :skip-provision true); create a single VM
  "
  ([base t & {:keys [skip-provision] :or {skip-provision false}}]
   (let [specs (map (fn [i] (update-in base [:machine :hostname] (fn [n] (str n "-" i)))) (range t))
         [_ m] (run (add systems specs) | (sys/create) | (block-wait) | (pretty-print "up"))]
     (when-not skip-provision
       (provision
        (with-ids
          (map (fn [[id _]] id) (:systems m)))))))

  ([s]
   (up s 1)))

(defn ssh-into
  "SSH into instances (open a terminal)"
  ([]
   (ssh-into (hosts)))
  ([{:keys [auth] :as hs}]
   (let [{:keys [user]} auth]
     (doseq [host (:hosts hs)]
       (.exec  (Runtime/getRuntime) (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~{user}@~{host}"))))))

