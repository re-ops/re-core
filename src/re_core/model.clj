(ns re-core.model
  "Model manipulation ns"
  (:require
   [re-share.config.core :refer (get! get*)]))

(def ^{:doc "A local binding of current environment (used for hypervisors, provisioners etc..)" :dynamic true :private true}
  env nil)

(defn set-dev
  "set root env to :dev"
  []
  (alter-var-root (var re-core.model/env) (fn [_] :default)))

(defmacro set-env [e & body] `(binding  [env ~e] ~@body))

(defn get-env! [] {:pre [(not (nil? env))]} env)

(def hypervizors
  #{:aws :physical :digital-ocean :kvm :lxc})

(def operations
  #{:reload :destroy :provision :stage :create :start :stop :clear :clone})

(defn figure-virt [spec]
  (first (filter hypervizors (keys spec))))

(defn hypervisor
  "obtains current environment hypervisor"
  [& ks]
  (apply get! :re-core :hypervisor ks))

(defn hypervisor*
  "obtains current environment hypervisor using get*"
  [& ks]
  (apply get* :re-core :hypervisor ks))

(defmulti clone
  "Clones an existing system map replacing unique identifiers in the process"
  (fn [spec clone-spec] (figure-virt spec)))

(defmulti translate
  "Converts general model to specific virtualization model"
  (fn [spec] (figure-virt spec)))

(defmulti vconstruct
  "Creates an hypervisor provider from input spec"
  (fn [spec] (figure-virt spec)))

(defmulti sconstruct
  "Creates an hypervisor synchronizer from input spec"
  (fn [k opts] k))

(defmulti check-validity (fn [m] [(figure-virt m) (or (:as m) :entity)]))
