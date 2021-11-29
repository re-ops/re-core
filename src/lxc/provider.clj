(ns lxc.provider
  "LXC provider for re-core"
  (:require
   [taoensso.timbre :as timbre]
   [lxc.client :as lxc]
   [lxc.sync :refer (sync-node)]
   [re-core.model :refer (vconstruct hypervisor* hypervisor sconstruct)]
   [re-core.provider :refer (selections mappings transform wait-for-ssh os->template into-mb into-description)]
   [re-core.core :refer (Sync Vm)]
   [clojure.spec.alpha :refer (valid?)]
   [clojure.core.incubator :refer (dissoc-in)]
   [lxc.spec :as spec]
   [re-core.persistency.systems :as s :refer (update-ip)]))

(timbre/refer-timbre)

(def timeout [1 :minute])

(defrecord Container [system-id node container user]
  Vm
  (create [this]
    (debug "creating container")
    (lxc/create node container)
    (lxc/add-description node container)
    (debug "container created")
    (lxc/start node container)
    (debug "container in running state")
    (lxc/wait-for-ip node container timeout)
    (let [ip (.ip this)]
      (wait-for-ssh ip user timeout)
      (update-ip system-id ip))
    this)

  (delete [this]
    (lxc/delete node container)
    (debug "container deleted"))

  (start [this]
    (when-not (= (.status this) "running")
      (lxc/start node container)
      (lxc/wait-for-ip node container timeout)
      (let [ip (.ip this)]
        (wait-for-ssh ip user timeout)
        (update-ip system-id ip))
      (debug "container started")))

  (stop [this]
    (lxc/stop node container)
    (s/put system-id (dissoc-in (s/get system-id) [:machine :ip]))
    (debug "container stopped"))

  (status [this]
    (when-let [s (lxc/status node container)]
      (name s)))

  (ip [this]
    (lxc/ip node container)))

(defn base [{:keys [image name] :as m}]
  {:name name
   :architecture "x86_64"
   :profiles ["default"]
   :devices {}
   :ephemeral false
   :config (select-keys m #{:limits.cpu :limits.ram})
   :source {:type "image" :alias image}})

(defn translate [machine]
  (-> machine
      (mappings {:os :image :hostname :name :cpu :limits.cpu :ram :limits.memory})
      (transform {:limits.cpu str :limits.memory (comp str into-mb)
                  :image (fn [img] (:template ((os->template :lxc) img)))})
      (base)))

(defrecord LXD [nodes opts]
  Sync
  (sync [this]
    (apply concat
           (map
            (fn [[k n]] (sync-node (mappings n {:username :user}))) nodes))))

(defmethod vconstruct :lxc [{:keys [lxc machine system-id] :as system}]
  {:post [(valid? :lxc/container  %)]}
  (let [node (hypervisor :lxc :nodes (lxc :node))
        auth (hypervisor :lxc :auth)
        container (assoc (translate machine) :description (into-description system))]
    (->Container system-id (merge node auth) container (:user machine))))

(defmethod sconstruct :lxc [_ opts]
  (LXD. (hypervisor :lxc :nodes) opts))

(comment
  (hypervisor :lxc :nodes :localhost))
