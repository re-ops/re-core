(ns kvm.provider
  "KVM virtualization provider for re-core"
  (:refer-clojure :exclude [sync])
  (:require
   [clojure.core.strint :refer (<<)]
   [re-share.config :refer (get!)]
   [com.rpl.specter :as spec :refer  (MAP-VALS ALL ATOM keypath)]
   [flatland.useful.map :refer (dissoc-in*)]
   [kvm.validations :refer (provider-validation)]
   [kvm.clone :refer (clone-domain)]
   [kvm.volumes :refer (clear-volumes create-volumes)]
   [kvm.common :refer (connect get-domain state domain-list)]
   [kvm.networking :refer (public-ip nat-ip update-ip)]
   [taoensso.timbre :as timbre]
   [es.systems :as s]
   [re-core.provider :refer (mappings selections transform os->template wait-for-ssh)]
   [re-share.core :refer (wait-for)]
   [kvm.sync :refer (descriptive-domains into-system)]
   [kvm.spice :refer (graphics remmina)]
   [hypervisors.networking :refer (set-hostname ssh-able?)]
   [re-core.core :refer (Sync Vm)]
   [re-core.model :refer (translate vconstruct sconstruct hypervisor* hypervisor)])
  (:import org.libvirt.LibvirtException))

(timbre/refer-timbre)

(declare ^:dynamic *libvirt-connection*)

(defn connection [{:keys [host user port]}]
  (connect (<< "qemu+ssh://~{user}@~{host}:~{port}/system")))

(defn c [] *libvirt-connection*)

(defmacro with-connection [& body]
  `(binding [*libvirt-connection* (connection ~'node)]
     (try
       (do ~@body)
       (finally
         (debug "status code for close libvirt connection" (.close *libvirt-connection*))))))

(defn wait-for-status
  "Waiting for VM status (timeout is in milliseconds)"
  [instance req-stat timeout]
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
            "Timed out on waiting for status"))

(def timeout [1 :minute])

(defn key- []
  (get! :shared :ssh :private-key-path))

(defprotocol Spicy
  (open-spice [this]))

(defrecord Domain [system-id node volumes domain type]
  Vm
  (create [this]
    (with-connection
      (clone-domain (c) domain {:type type :node node :system-id system-id})
      (debug "clone done")
      (create-volumes (c) (domain :name) volumes)
      (debug "volumes created")
      (wait-for-status this "running" timeout)
      (debug "in running state")
      (let [ip (.ip this) flavor (get-in domain [:image :flavor])
            {:keys [user name hostname]} domain]
        (wait-for-ssh ip user timeout)
        (set-hostname hostname name {:user user :host ip :ssh-key (key-)} flavor)
        (update-ip system-id ip)
        this)))

  (delete [this]
    (with-connection
      (clear-volumes (c) (domain :name) volumes)
      (.undefine (get-domain (c) (domain :name)))))

  (start [this]
    (with-connection
      (when-not (= (.status this) "running")
        (.create (get-domain (c) (domain :name)))
        (when (ssh-able? (get-in domain [:image :flavor]))
          (let [ip (.ip this)]
            (wait-for-ssh ip (domain :user) timeout)
            (update-ip system-id ip))))))

  (stop [this]
    (with-connection
      (s/put system-id
             (dissoc-in* (s/get system-id) [:machine :ip]))
      (.destroy (get-domain (c) (domain :name)))
      (wait-for-status this "shutoff" timeout)))

  (status [this]
    (with-connection
      (try
        (if-not (first (filter #(= % (domain :name)) (domain-list (c))))
          false
          (state (get-domain (c) (domain :name))))
        (catch LibvirtException e (error (.getMessage e)) false))))

  (ip [this]
    (with-connection
      (if (= (:host node) "localhost")
        ; in case that the node is local host we can't ssh to our public ip!
        (nat-ip (c) (domain :name) node)
        (public-ip (c) (domain :user) node (domain :name)))))

  Spicy
  (open-spice [this]
    (with-connection
      (remmina domain (graphics (c) (domain :name))))))

(defn sync-node [k node opts]
  (with-connection
    (map (partial into-system (c)) (descriptive-domains (c)))))

(defrecord Libvirt [nodes opts]
  Sync
  (sync [this]
    (apply concat
           (map
            (fn [[k n]] (sync-node k (mappings n {:username :user}) opts)) nodes))))

(defn machine-ts
  "Construcuting machine transformations"
  [{:keys [hostname domain] :as machine}]
  {:name (fn [hostname] hostname)
   :image (fn [os] ((os->template :kvm) os))})

(defmethod translate :kvm [{:keys [machine kvm] :as spec}]
  (-> machine
      (mappings {:os :image :hostname :name})
      (transform (machine-ts machine))
      (assoc :hostname (machine :hostname))
      (selections [[:name :user :image :cpu :ram :hostname]])))

(defn node-m [node]
  (assoc (hypervisor* :kvm :nodes node) :name node))

(defn pool-m [node pool]
  (merge {:id (name pool)} (((node-m node) :pools) pool)))

(defmethod vconstruct :kvm [{:keys [kvm machine system-id type] :as spec}]
  (let [[domain] (translate spec) {:keys [node volumes]} kvm
        node* (dissoc (mappings (node-m node) {:username :user}) :pools)
        volumes* (spec/transform [ALL (keypath :pool)] (partial pool-m node) volumes)]
    (provider-validation domain node*)
    (->Domain system-id node* volumes* domain type)))

(defmethod sconstruct :kvm [_ opts]
  (Libvirt. (hypervisor :kvm :nodes) opts))

(comment
  (hypervisor :kvm))
