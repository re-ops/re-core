(ns kvm.provider
  (:require
   [flatland.useful.map :refer (dissoc-in*)]
   [safely.core :refer [safely]]
   [kvm.validations :refer (provider-validation)]
   [clojure.core.strint :refer (<<)]
   [kvm.clone :refer (clone-domain)]
   [kvm.disks :refer (clear-volumes create-volume delete-volume)]
   [kvm.common :refer (connect get-domain domain-zip state domain-list)]
   [kvm.networking :refer (public-ip nat-ip update-ip)]
   [re-mote.sshj :refer (ssh-up?)]
   [re-core.core :refer (Vm)]
   [taoensso.timbre :as timbre]
   [re-core.persistency.systems :as s]
   [re-core.provider :refer (mappings selections transform os->template wait-for wait-for-ssh)]
   [hypervisors.networking :refer (set-hostname)]
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
  `(let [~'connection (connection ~'node)]
     (do ~@body)))

(defn wait-for-status
  "Waiting for ec2 machine status timeout is in mili"
  [instance req-stat timeout]
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
            {:type ::kvm:status-failed :status req-stat :timeout timeout}
            "Timed out on waiting for status"))

(defrecord Domain [system-id node domain]
  Vm
  (create [this]
    (with-connection
      (let [image (get-in domain [:image :template]) target (select-keys domain [:name :cpu :ram])]
        (clone-domain connection image target)
        (debug "clone done")
        (wait-for-status this "running" [5 :minute])
        (debug "in running state")
        (let [ip (.ip this) flavor (get-in domain [:image :flavor])]
          (wait-for-ssh ip (domain :user) [5 :minute])
          (set-hostname (domain :hostname) (domain :name) {:user (domain :user) :host ip} flavor)
          (update-ip system-id ip)
          this))))

  (delete [this]
    (with-connection
      (clear-volumes connection (domain-zip connection (domain :name)))
      (.undefine (get-domain connection (domain :name)))))

  (start [this]
    (with-connection
      (when-not (= (.status this) "running")
        (.create (get-domain connection (domain :name)))
        (let [ip (.ip this)]
          (wait-for-ssh ip (domain :user) [5 :minute])
          (update-ip system-id ip)))))

  (stop [this]
    (with-connection
      (s/update-system system-id
         (dissoc-in* (s/get-system system-id) [:machine :ip]))
      (.destroy (get-domain connection (domain :name)))
      (wait-for-status this "shutoff" [5 :minute])))

  (status [this]
    (with-connection
      (try
        (if-not (first (filter #(= % (domain :name)) (domain-list connection)))
          false
          (state (get-domain connection (domain :name))))
        (catch LibvirtException e (debug (.getMessage e)) false))))

  (ip [this]
    (with-connection
      (if (= (:host node) "localhost")
        (nat-ip connection (domain :name) node)
        (public-ip connection (domain :user) node (domain :name))))))

(defn machine-ts
  "Construcuting machine transformations"
  [{:keys [hostname domain] :as machine}]
  {:name (fn [hostname] (<< "~{hostname}.~{domain}"))
   :image (fn [os] ((os->template :kvm) os))})

(defmethod translate :kvm [{:keys [machine kvm] :as spec}]
  (-> machine
      (mappings {:os :image :hostname :name})
      (transform (machine-ts machine))
      (assoc :hostname (machine :hostname))
      (selections [[:name :user :image :cpu :ram :hostname]])))

(defmethod vconstruct :kvm [{:keys [kvm machine system-id] :as spec}]
  (let [[domain] (translate spec) {:keys [node]} kvm
        node* (mappings (hypervisor* :kvm :nodes node) {:username :user})]
    (provider-validation domain node*)
    (->Domain system-id node* domain)))

(comment
  (create-volume (connection {:host "localhost" :user "ronen" :port 22}) "daemon" 100 "/media/daemon/" "foo")
  (delete-volume (connection {:host "localhost" :user "ronen" :port 22}) "daemon" "foo")
  (domain-list (connection {:host "localhost" :user "ronen" :port 22}))
  (get-domain (connection {:host "localhost" :user "ronen" :port 22}) "red1-.local"))
