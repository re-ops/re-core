(ns lxc.provider
  "LXC provider for re-core"
  (:require
   [taoensso.timbre :as timbre]
   [lxc.client :as lxc]
   [re-core.model :refer (vconstruct hypervisor* hypervisor)]
   [re-core.provider :refer (selections mappings transform wait-for-ssh os->template)]
   [re-core.core :refer (Sync Vm)]
   [clojure.spec.alpha :refer (valid?)]
   [flatland.useful.map :refer (dissoc-in*)]
   [lxc.spec :as spec]
   [es.systems :as s :refer (update-ip)]))

(timbre/refer-timbre)

(def timeout [1 :minute])

(defrecord Container [system-id node container user]
  Vm
  (create [this]
    (lxc/create node container)
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
    (s/put system-id (dissoc-in* (s/get system-id) [:machine :ip]))
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
      (transform {:limits.cpu str :limits.memory str
                  :image (fn [img] (:template ((os->template :lxc) img)))})
      (base)))

(defmethod vconstruct :lxc [{:keys [lxc machine system-id] :as spec}]
  {:post [(valid? :lxc/container  %)]}
  (let [node (hypervisor :lxc :nodes (lxc :node))
        auth (hypervisor :lxc :auth)
        container (translate machine)]
    (->Container system-id (merge node auth) container (:user machine))))

(comment
  (hypervisor :lxc :nodes :localhost))
