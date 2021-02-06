(ns re-core.repl
  "Repl Driven re-core"
  (:refer-clojure :exclude [list update sync])
  (:require
   [re-core.model :refer (figure-virt)]
   [re-core.repl.terminal :refer (launch-ssh)]
   [es.history :refer (refer-history)]
   [clojure.core.strint :refer  (<<)]
   [re-core.repl.base :refer (refer-base)]
   [re-core.repl.systems :refer (refer-systems)]
   [re-core.presets.systems :as sp]
   [re-core.presets.types :as tp]
   [re-share.config.core :as c]
   [re-core.repl.types :refer (refer-types)])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)
(refer-history)
(refer-types)

(def systems (Systems.))
(def types (Types.))

; Filtering functions

(defn hyp
  "Get instances by hypervisor type:
     (start (hyp :lxc))"
  [v]
  (fn [[_ m]]
    (= (figure-virt m) v)))

(defn typed
  "Get instances by type:
     (start (by-type :redis))"
  [t]
  (fn [[_ {:keys [type] :as m}]] (=  type t)))

(defn ip
  "Pick systems that has ip addresses (they are running) 
     (stop ip)"
  [[_ {:keys [machine] :as m}]]
  (machine :ip))

(defn with-ips
  "Pick systems with specific ips:
     (stop (with-ip \"10.0.0.4\"))"
  [ips]
  (fn [[_ {:keys [machine]}]]
    ((into #{} ips) (machine :ip))))

(defn with-ids
  "Pick systems using multiple ids:
     (provision (with-ids [\"Bar\" \"Foo\"]))"
  [ids]
  (fn [[id _]]
    ((into #{} (map str ids)) (str id))))

(defn matching
  "Match instances by partial id matching (ala git):
     (provision (matching \"A17_\"))"
  [part]
  {:pre [(not (clojure.string/blank? part))]}
  (fn [[id _]] (.contains id part)))

(defn named
  "Match instances by hostname matching:
     (provision (named \"foo\"))"
  [names]
  (fn [[_ {:keys [machine]}]] ((into #{} names) (machine :hostname))))

(defn match-kv
  "Match instances by kv pair:
     (provision (match-kv [:machine :os] :ubuntu-18.04))"
  [ks v]
  (fn [[_ m]] (=  (get-in m ks) v)))

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
  "Clear only the model (VM won't be deleted):
     (clear) ; clear all systems (both runnging and non running)
     (clear (by-type :redis)) ; clear systems with redis type
     (clear identity :types) ; clear all types
     (clear (fn [[t _]] (= t  \"redis\") :types)) ; clear the redis type
  "
  ([]
   (clear identity :systems {}))
  ([f]
   (clear f :systems {}))
  ([f on]
   (clear f on {}))
  ([f on opts]
   (case on
     :systems (run (ls systems) | (filter-by f) | (ack opts) | (rm) | (pretty))
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
   (halt f {}))
  ([f opts]
   (run (ls systems) | (filter-by f) | (ack opts) | (sys/stop) | (async-wait pretty-print "halt"))))

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
     (list identity :types) ; list all types
     (list ip :systems print? false); return matched systems without printing them
  "
  ([]
   (list identity :systems))
  ([f]
   (list f :systems))
  ([f on & {:keys [print?] :or {print? true}}]
   (if print?
     (case on
       :systems (run (ls systems) | (filter-by f) | (pretty))
       :types (run (ls types) | (pretty)))
     (case on
       :systems (run (ls systems) | (filter-by f))
       :types (ls types)))))

(defn hosts
  "Convert systems into re-mote hosts:

    All systems using ip address:

     (hosts)

    Use re-gent addresses by grabbing hostname

     (cpu-persist (hosts ip :hostname)))

    All redis instances using hostname

     (hosts (by-type :redis) :hostname) "
  ([]
   (hosts ip :ip))
  ([f k]
   (run (ls systems) | (filter-by f) | (into-hosts k))))

(defn provision
  "Provision VM:

    Provision all running instances:

     (provision)

    Provision using filter fn:

     (provision (fn [{:keys [type]] (= type :redis)))
   "
  ([]
   (provision ip))
  ([f]
   (run (ls systems) | (filter-by f) | (sys/provision) | (async-wait pretty-print "provision"))))

(defn- create-system
  "Create a system internal implementation"
  [base args]
  (run (valid? systems base args) | (add-) | (sys/create) | (async-wait pretty-print "create")))

(defn- create-type
  "Create type internal implementation"
  [base args]
  (let [ms (tp/validate (tp/materialize-preset base args))]
    (if-not (empty? (ms false))
      (ms false)
      (run (add- types [(ms true)]) | (pretty)))))

(defn create
  "Creating system/type instances by using presets to quickly pick their properties.

   Kvm instance using defaults user using c1-medium ram/cpu allocation:

     (create kvm defaults c1-medium local :redis)

   5 instance in one go:

     (create kvm defaults c1-medium local :redis 5)

   Using a custom hostname:

     (create kvm defaults c1-medium local \"furry\" :redis)

   Using a KVM volume:

     (create kvm defaults c1-medium local vol-128G :redis)

   Custom os (using default machine):

     (create kvm default-machine c1-medium (os :ubuntu-18.04-dekstop) :redis)

   Type instances:
     (create cog 're-cipes.profiles/osquery default-src :osquery \"osquery type\") ; using default src directory
  "
  [base & args]
  (cond
    (:machine base) (create-system base args)
    (:cog base) (create-type base args)
    :else (throw (ex-info "No macthing creation logic found" {:base base :args args}))))

(defn add
  "Add existing system instances:
     (add kvm default-machine local large (os :ubuntu-desktop-20.04) (with-host \"foo\") :base \"Existing base instance\")"
  [base & args]
  (let [{:keys [fns total type hostname]} (sp/into-spec {} args)
        transforms [(sp/with-type type) (sp/with-host hostname)]
        all (apply conj transforms fns)
        specs (map  (fn [_] (reduce (fn [m f] (f m)) base all)) (range (or total 1)))]
    (run (valid? systems base args) | (add-) | (pretty-print "add"))))

(defn sync
  "Sync existing instances into re-core systems:
     (sync :digital-ocean)
     (sync :kvm :active true) ; using options
     (sync :aws :filter (fn [m] ...)) ; using a filtering function
     (sync :physical {:pivot rosetta  :network \"192.168.1.0/24\" :user \"re-ops\"}) ; nmap based sync "
  ([hyp]
   (sync hyp {}))
  ([hyp opts]
   (run (synch systems hyp opts) | (pretty))))

(defn fill
  "fill systems information from provided ks v pair
    (fill identity [:machine :os] :ubuntu-18.04)
  "
  [f ks v]
  (run (ls systems) | (filter-by f) | (update-systems ks v)))

(defn ssh-into
  "SSH into instances (open a terminal window):
     (ssh-into)"
  ([]
   (ssh-into identity))
  ([f]
   (let [{:keys [auth] :as hs} (hosts f :ip)]
     (doseq [host (:hosts hs)]
       (let [target (<< "~(auth :user)@~{host}") private-key (c/get! :shared :ssh :private-key-path)]
         (launch-ssh target private-key))))))

(defn spice-into
  "Open remote spice connection to KVM instances (using remmina spice):
     (spice-into (by-type :desktop))"
  ([]
   (spice-into identity))
  ([f]
   (run (ls systems) | (filter-by f) | (filter-by (fn [[_ m]] (contains? m :kvm))) | (spice) | (pretty-print "spice"))))
