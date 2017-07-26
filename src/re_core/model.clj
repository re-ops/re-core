(ns re-core.model
  "Model manipulation ns"
  (:require
   [re-core.common :refer (get! get*)]))

(def ^{:doc "A local binding of current environment (used for hypervisors, provisioners etc..)" :dynamic true :private true}
  env nil)

(defn set-dev
  "set root env to :dev"
  []
  (alter-var-root (var re-core.model/env) (fn [_] :dev)))

(defmacro set-env [e & body] `(binding  [env ~e] ~@body))

(defn get-env! [] {:pre [env]} env)

(def hypervizors
  #{:aws :physical :digital-ocean :gce :kvm})

(def operations
  #{:reload :destroy :provision :stage :create :start :stop :clear :clone})

(defn figure-virt [spec] (first (filter hypervizors (keys spec))))

(defn hypervisor
  "obtains current environment hypervisor"
  [& ks] {:pre [env]}
  (apply get! :hypervisor env ks))

(defn hypervisor*
  "obtains current environment hypervisor using get*"
  [& ks] {:pre [env]}
  (apply get* :hypervisor env ks))

(defn- select-sub
  "select sub map"
  [m ks]
  (reduce
   (fn [r k] (if-let [v (get-in m k)] (assoc-in r k v) r)) {} ks))

(def whitelist
  [[:gce :ostemplates] [:digital-ocean]
   [:aws] [:physical] [:openstack]
   [:kvm :ostemplates] [:kvm :nodes]])

(defn sanitized-envs
  "sanitized (from sensative data) environments "
  [envs]
  (let [es (filter envs (keys (get* :hypervisor)))
        inc-nodes (map (fn [e] (select-sub (get* :hypervisor) (map #(into [e] %) whitelist))) es)
        sanitized  [:ssh-port :username :password :access-key :secret-key]]
    (apply merge (clojure.walk/postwalk #(if (map? %) (apply dissoc % sanitized) %) inc-nodes))))

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

(defmulti pconstruct
  "Creates a Provisioner instance model from input spec"
  (fn [type spec] (first (filter provisioners (keys type)))))

(def remoters #{:re-mote :capistrano :ruby})

(defn figure-rem [spec] (first (filter remoters (keys spec))))

(defmulti rconstruct
  "Creates a Remoter instance model from input spec"
  (fn [action run-info]
    (figure-rem action)))

