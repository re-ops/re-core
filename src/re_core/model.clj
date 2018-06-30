(ns re-core.model
  "Model manipulation ns"
  (:require
   [re-share.config :refer (get! get*)]))

(def ^{:doc "A local binding of current environment (used for hypervisors, provisioners etc..)" :dynamic true :private true}
  env nil)

(defn set-dev
  "set root env to :dev"
  []
  (alter-var-root (var re-core.model/env) (fn [_] :default)))

(defmacro set-env [e & body] `(binding  [env ~e] ~@body))

(defn get-env! [] {:pre [(not (nil? env))]} env)

(def hypervizors
  #{:aws :physical :digital-ocean :kvm})

(def operations
  #{:reload :destroy :provision :stage :create :start :stop :clear :clone})

(defn figure-virt [spec] (first (filter hypervizors (keys spec))))

(defn hypervisor
  "obtains current environment hypervisor"
  [& ks]
  (apply get! :re-core :hypervisor env ks))

(defn hypervisor*
  "obtains current environment hypervisor using get*"
  [& ks] {:pre [(not (nil? env))]}
  (apply get* :re-core :hypervisor env ks))

(defn- select-sub
  "select sub map"
  [m ks]
  (reduce
   (fn [r k] (if-let [v (get-in m k)] (assoc-in r k v) r)) {} ks))

(def whitelist
  [[:digital-ocean]
   [:aws] [:physical] [:openstack]
   [:kvm :ostemplates] [:kvm :nodes]])

(defmulti clone
  "Clones an existing system map replacing unique identifiers in the process"
  (fn [spec clone-spec] (figure-virt spec)))

(defmulti translate
  "Converts general model to specific virtualization model"
  (fn [spec] (figure-virt spec)))

(defmulti vconstruct
  "Creates a Virtualized instance model from input spec"
  (fn [spec] (figure-virt spec)))

(defmulti check-validity (fn [m] [(figure-virt m) (or (:as m) :entity)]))

(def provisioners #{:chef :puppet})

(def remoters #{:re-mote :capistrano :ruby})

(defn figure-rem [spec] (first (filter remoters (keys spec))))
