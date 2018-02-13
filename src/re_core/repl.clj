(ns re-core.repl
  "Repl Driven re-core"
  (:refer-clojure :exclude [list update])
  (:require
   [clojure.core.strint :refer  (<<)]
   [re-core.repl.base :refer (refer-base)]
   [re-core.repl.systems :refer (refer-systems)]
   [re-core.presets.system :as sp]
   [re-core.presets.type :as tp]
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

; filtering functions

(defn by-type
  "get instances by type"
  [t]
  (fn [[_ {:keys [type] :as m}]] (=  type t)))

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
   (run (ls systems) | (filter-by f) | (sys/reload) | (async-wait pretty-print "reload"))))

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
     (destroy (matching \"Fstr\")) ; remove all instances with an id containing Fstr
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
   (run (ls systems) | (filter-by f) | (sys/start) | (async-wait pretty-print "start"))))

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

(defn matching
  "Match instances by partial id matching"
  [part]
  (fn [[id _]] (.contains id part)))

(defn provision
  "Provision VM:
    (provision) ; run provision on all running instances
    (provision (fn [{:keys [type]] (= type :redis))) ; provision using filter fn"
  ([]
   (provision ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/provision))))

(defn- create-system [base args]
  (let [{:keys [fns total type hostname]} (sp/into-spec {} args)
        transforms [(sp/with-type type) (sp/with-host hostname) sp/name-gen]
        all (apply conj transforms fns)
        specs (map  (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))]
    (run (add- systems specs) | (sys/create) | (async-wait pretty-print "create"))))

(defn- create-type [base args]
  (let [{:keys [fns type description]} (tp/into-spec {} args)
        transforms [(tp/with-type type) (tp/with-desc description)]
        spec (reduce (fn [m f] (f m)) base (apply conj transforms fns))]
    (run (add- types [spec]) | (pretty))))

(defn create
  "Create instances
     (create kvm-small :redis) ; Create a small kvm instance that run redis
     (create kvm-small :redis \"furry\") ; Create a small kvm instance with a hostname
     (create kvm-small vol-128G :redis 5) ; Create 5 small redis instances with a 100G Volume
     (create kvm-small vol-128G :redis 5 \"blurby\") ; Each with 100 GB volume
     (create puppet src :redis \"redis instance type\") ; Puppet based type using local src directory "
  [base & args]
  (cond
    (:machine base) (create-system base args)
    (:puppet base) (create-type base args)
    :else (throw (ex-info "creation type not found" {:base base :args args}))))

(defn add
  "Add existing system instances:
     (add (kvm-size 1 512 :openbsd) \"furby\" :foo); we can specify an os
   "
  [base & args]
  (let [{:keys [fns total type hostname]} (sp/into-spec {} args)
        transforms [(sp/with-type type) (sp/with-host hostname)]
        all (apply conj transforms fns)
        specs (map  (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))]
    (run (add- systems specs) | (pretty-print "add"))))

(defn ssh-into
  "SSH into instances (open a terminal)"
  ([]
   (ssh-into identity))
  ([f]
   (let [{:keys [auth] :as hs} (hosts f :ip)]
     (doseq [host (:hosts hs)]
       (.exec  (Runtime/getRuntime) (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~(auth :user)@~{host}"))))))

