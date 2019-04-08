(ns lxc.provider
  "LXC provider for re-core"
  (:require
   [clojure.string :refer (lower-case)]
   [taoensso.timbre :as timbre]
   [lxc.client :as lxc]
   [re-core.model :refer (vconstruct hypervisor* hypervisor)]
   [re-core.provider :refer (selections mappings transform wait-for-ssh os->template)]
   [re-core.core :refer (Sync Vm)]
   [clojure.spec.alpha :as s]
   [lxc.spec :as spec]
   [es.systems :refer (update-ip)]))

(timbre/refer-timbre)

(def timeout [1 :minute])

(defrecord Container [system-id node container user]
  Vm
  (create [this]
    (lxc/create node container)
    (debug "container created")
    (lxc/start node container)
    (debug "container in running state")
    (let [ip (.ip this)]
      (wait-for-ssh ip user timeout)
      (update-ip system-id ip)
      this))

  (delete [this]
    (lxc/delete node container)
    (debug "container deleted"))

  (start [this]
    (lxc/start node container)
    (debug "container started"))

  (stop [this]
    (lxc/stop node container)
    (debug "container stopped"))

  (status [this]
    (when-let [s (lxc/status node container)]
      (name s)))

  (ip [this]
    (lxc/ip node container)))

(def base {:architecture "x86_64"
           :profiles ["default"]
           :devices {}
           :ephemeral false
           :config {:limits.cpu "2"}
           :source {:type "image" :alias "ubuntu-18.04"}})

(defn devices [m]
  (assoc m :config (select-keys m #{:limits.cpu :limits.ram})))

(defn translate [{:keys [machine]}]
  (-> machine
      (mappings {:os :image :hostname :name :cpu :limits.cpu :ram :limits.ram})
      (devices)
      (transform {:image (fn [img] ((os->template :lxc) img))})
      (selections [[:name :user :image :config]])
      (merge base)))

(defmethod vconstruct :lxc [{:keys [lxc machine system-id type] :as spec}]
  {:post [(s/valid? :lxc/container  %)]}
  (let [node (hypervisor :lxc :nodes (lxc :node))
        auth (hypervisor :lxc :auth)
        container (translate spec)]
    (->Container system-id (merge node auth) container (:user machine))))

(comment
  (hypervisor :lxc :nodes :localhost))
