(ns kvm.provider
  (:require
   [com.rpl.specter :as spec :refer  (MAP-VALS ALL ATOM keypath)]
   [flatland.useful.map :refer (dissoc-in*)]
   [safely.core :refer [safely]]
   [kvm.validations :refer (provider-validation)]
   [clojure.core.strint :refer (<<)]
   [kvm.clone :refer (clone-domain)]
   [kvm.volumes :refer (clear-volumes create-volumes)]
   [kvm.common :refer (connect get-domain state domain-list)]
   [kvm.networking :refer (public-ip nat-ip update-ip)]
   [re-core.core :refer (Vm)]
   [taoensso.timbre :as timbre]
   [es.systems :as s]
   [re-core.provider :refer (mappings selections transform os->template wait-for-ssh)]
   [re-share.core :refer (wait-for)]
   [hypervisors.networking :refer (set-hostname ssh-able?)]
   [re-core.model :refer (translate vconstruct hypervisor*)])
  (:import org.libvirt.LibvirtException))

(timbre/refer-timbre)

(defn connection [{:keys [host user port]}]
  (safely
   (connect (<< "qemu+ssh://~{user}@~{host}:~{port}/system"))
   :on-error
   :log-errors false
   :max-retry 5
   :message "Error while trying to connect to libvirt"
   :retry-delay [:random-range :min 200 :max 500]))

(defmacro with-connection [& body]
  `(let [~'c (connection ~'node)]
     (do ~@body)))

(defn wait-for-status
  "Waiting for ec2 machine status timeout is in mili"
  [instance req-stat timeout]
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
            "Timed out on waiting for status"))

(defrecord Domain [system-id node volumes domain]
  Vm
  (create [this]
    (with-connection
      (let [image (get-in domain [:image :template]) target (select-keys domain [:name :cpu :ram])]
        (clone-domain c image target)
        (debug "clone done")
        (create-volumes c (domain :name) volumes)
        (debug "volumes created")
        (wait-for-status this "running" [5 :minute])
        (debug "in running state")
        (let [ip (.ip this) flavor (get-in domain [:image :flavor])]
          (wait-for-ssh ip (domain :user) [5 :minute])
          (set-hostname (domain :hostname) (domain :name) {:user (domain :user) :host ip} flavor)
          (update-ip system-id ip)
          this))))

  (delete [this]
    (with-connection
      (clear-volumes c (domain :name) volumes)
      (.undefine (get-domain c (domain :name)))))

  (start [this]
    (with-connection
      (when-not (= (.status this) "running")
        (.create (get-domain c (domain :name)))
        (when (ssh-able? (get-in domain [:image :flavor]))
          (let [ip (.ip this)]
            (wait-for-ssh ip (domain :user) [5 :minute])
            (update-ip system-id ip))))))

  (stop [this]
    (with-connection
      (s/put system-id
             (dissoc-in* (s/get system-id) [:machine :ip]))
      (.destroy (get-domain c (domain :name)))
      (wait-for-status this "shutoff" [5 :minute])))

  (status [this]
    (with-connection
      (try
        (if-not (first (filter #(= % (domain :name)) (domain-list c)))
          false
          (state (get-domain c (domain :name))))
        (catch LibvirtException e (debug (.getMessage e)) false))))

  (ip [this]
    (with-connection
      (if (= (:host node) "localhost")
        (nat-ip c (domain :name) node)
        (public-ip c (domain :user) node (domain :name))))))

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
  (hypervisor* :kvm :nodes node))

(defn pool-m [node pool]
  (merge {:id (name pool)} (((node-m node) :pools) pool)))

(defmethod vconstruct :kvm [{:keys [kvm machine system-id] :as spec}]
  (let [[domain] (translate spec) {:keys [node volumes]} kvm
        node* (dissoc (mappings (node-m node) {:username :user}) :pools)
        volumes* (spec/transform [ALL (keypath :pool)] (partial pool-m node) volumes)]
    (provider-validation domain node*)
    (->Domain system-id node* volumes* domain)))

(comment
  (def c (connection {:host "localhost" :user "ronen" :port 22}))

  (create-volume c "default" 10 "/var/lib/libvirt/images/" "foo.img")

  (clojure.pprint/pprint
   (.delete (:volume (second (kvm.volumes/list-volumes c "reops-0.local"))) 0))

  (def d
    (get-domain c "reops-0.local"))

  (attach d "/var/lib/libvirt/images/foo.img")

  (delete-volume c "default" "foo.img")

  (domain-list c)

  (get-domain c "red1.local"))
