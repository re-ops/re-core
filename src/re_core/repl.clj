(ns re-core.repl
  "Repl Driven re-core"
  (:refer-clojure :exclude [list update sync])
  (:require
   [es.history :refer (refer-history)]
   [clojure.core.strint :refer  (<<)]
   [re-core.repl.base :refer (refer-base)]
   [re-core.repl.systems :refer (refer-systems)]
   [re-core.presets.common :as sp]
   [re-share.config :as c]
   [re-core.presets.type :as tp]
   [re-core.repl.types :refer (refer-types)]
   [taoensso.timbre :as timbre])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)
(refer-history)
(refer-types)

(def systems (Systems.))
(def types (Types.))

; filtering functions

(defn by-type
  "Get instances by type:
     (reload (by-type :redis))"
  [t]
  (fn [[_ {:keys [type] :as m}]] (=  type t)))

(defn ip
  "Pick systems with an ip (they are running):
     (stop ip)"
  [[_ {:keys [machine] :as m}]]
  (machine :ip))

(defn with-ids
  "Pick systems using unique ids:
     (provision (with-ids \"Bar\" \"Foo\"))"
  [& ids]
  (fn [[id _]]
    ((into #{} (map str ids)) (str id))))

(defn matching
  "Match instances by partial id matching (ala git):
     (provision (matching \"A17_\"))"
  [part]
  (fn [[id _]] (.contains id part)))

; management

(defn reload
  "Reload (stop destroy and up):
     (reload) ; reload all running instances
     (reload (by-type :redis)) ; reload all redis instances"
  ([]
   (reload ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/reload) | (async-wait pretty-print "reload"))))

(defn clear
  "Clear model only (VM won't be deleted):
     (clear) ; clear all systems (both runnging and non running)
     (clear (by-type :redis)) ; clear systems with redis type
     (clear identity :types) ; clear all types"
  ([]
   (clear identity))
  ([f]
   (clear f :systems))
  ([f on]
   (case on
     :systems (run (ls systems) | (filter-by f) | (rm) | (pretty))
     :types (run (ls types) | (filter-by f) | (rm) | (pretty)))))

(defn destroy
  "Destroy instances (both clear and remove VM):
     (destroy) ; remove all instances (both running and non running)
     (destroy ip) ; remove running instances only
     (destroy (matching \"Fstr\")) ; remove all instances with an id containing Fstr
     (destroy ip {:force true}) ; remove running instances only without confirmation"
  ([]
   (destroy identity {}))
  ([f]
   (destroy f {}))
  ([f opts]
   (run (ls systems) | (filter-by f) | (ack opts) | (sys/destroy) | (async-wait pretty-print "destroy"))))

(defn halt
  "Halt instances:
     (halt) ; halt all running (have ip)
   "
  ([]
   (halt ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/stop) | (async-wait pretty-print "halt"))))

(defn start
  "Start instances:
     (start) ; start all without ip (stopped)
     (start (by-type :redis)) ; start all redis types"
  ([]
   (start (comp not ip)))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/start) | (async-wait pretty-print "start"))))

(defn list
  "List available instances:
     (list) ; list all systems
     (list ip) ; list all systems that have an ip (running)
     (list identity :types) ; list all types"
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
     (cpu-persist (hosts ip :hostname))) ; use re-gent addresses by grabbing hostname
     (hosts (by-type :redis) :hostname) ; all redis instances using hostname"
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
   (run (ls systems) | (filter-by f) | (sys/provision))))

(defn- create-system
  "Create a system internal implementation"
  [base args]
  (let [{:keys [fns total type hostname]} (sp/into-spec {} args)
        transforms [(sp/with-type type) (sp/with-host hostname) sp/name-gen]
        all (apply conj transforms fns)
        specs (map  (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))]
    (run (add- systems specs) | (sys/create) | (async-wait pretty-print "create"))))

(defn- create-type
  "Create type internal implementation"
  [base args]
  (let [{:keys [fns type description]} (tp/into-spec {} args)
        transforms [(tp/with-type type) (tp/with-desc description)]
        spec (reduce (fn [m f] (f m)) base (apply conj transforms fns))]
    (run (add- types [spec]) | (pretty))))

(defn create
  "A function for creating instances, System instances:
     (create kvm-small :redis) ; kvm instance that run redis
     (create kvm-small :redis 5) ; creating 5 in one go
     (create kvm-small :redis \"furry\") ; with custom hostname (default generated from type)
     (create kvm-small vol-128G :redis) ; 128G Volume
     (create kvm-small :redis (os :ubuntu-16.04-dekstop)) ; custom os type
   Type instances:
     (create puppet default-src :redis \"redis type\") ; using default src directory
     (create puppet (src \"/home/foo/redis-sandbox\") :redis \"redis type\") ; using local src directory
     (create puppet (args \"--hiera_config\" \"hiera.yml\" \"manifests/default.pp\") :redis \"redis type\") ; with args
  "
  [base & args]
  (cond
    (:machine base) (create-system base args)
    (:puppet base) (create-type base args)
    (:re-conf base) (create-type base args)
    :else (throw (ex-info "creation type not found" {:base base :args args}))))

(defn add
  "Add existing system instances:
      (add (kvm-size 1 512) :ubuntu-16.04-desktop \"furby\" :foo); we specify an os"
  [base & args]
  (let [{:keys [fns total type hostname]} (sp/into-spec {} args)
        transforms [(sp/with-type type) (sp/with-host hostname)]
        all (apply conj transforms fns)
        specs (map  (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))]
    (run (add- systems specs) | (pretty-print "add"))))

(defn sync
  "Sync an existing hypervisor state into re-core:
     (sync :digital-ocean)
     (sync :kvm :active true) ; using options
     (sync :aws :filter (fn [m] ...)) ; using a filtering function"
  ([hyp]
   (sync hyp {}))
  ([hyp opts]
   (run (synch systems hyp opts) | (pretty))))

(defn ssh-into
  "SSH into instances (open a terminal window):
     (ssh-into)"
  ([]
   (ssh-into identity))
  ([f]
   (let [{:keys [auth] :as hs} (hosts f :ip)]
     (doseq [host (:hosts hs)]
       (let [target (<< "~(auth :user)@~{host}") private-key (c/get! :shared :ssh :private-key-path)]
         (.exec (Runtime/getRuntime)
                (<< "/usr/bin/x-terminal-emulator --disable-factory -e /usr/bin/ssh ~{target} -i ~{private-key}")))))))

(defn spice-into
  "Open remote spice connection to KVM instances (using remmina spice):
     (spice-into (by-type :desktop))"
  ([]
   (spice-into identity))
  ([f]
   (run (ls systems) | (filter-by f) | (filter-by (fn [[_ m]] (contains? m :kvm))) | (spice) | (pretty-print "spice"))))
