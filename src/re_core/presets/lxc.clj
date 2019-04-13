(ns re-core.presets.lxc
  "LXC presets"
  (:require
   [re-core.presets.common :refer (os machine)]
   [clojure.core.strint :refer (<<)]))

(defn lxc [machine node]
  {:machine machine :lxc {:node node}})

(defn lxc-machine [cpu ram os]
  (merge {:cpu cpu :ram ram} (machine "re-ops" "local" os)))

(def default-node :remote)

(defn node [n]
  (fn [container]
    (assoc-in container [:lxc :node] n)))

(defn lxc-size
  ([cpu ram]
   (lxc-size cpu ram :ubuntu-18.04))
  ([cpu ram os]
   (lxc (lxc-machine cpu ram os) default-node)))

; Default lxc container types
(def ^{:doc "Tiny LXC container"} lxc-tiny (lxc-size 1 512))

(def ^{:doc "Small LXC container"} lxc-small (lxc-size 2 1024))

(def ^{:doc "Medium LXC container"} lxc-medium (lxc-size 4 4096))

(def ^{:doc "Large LXC container"} lxc-large (lxc-size 8 8192))

(def ^{:doc "Extra large LXC container"} lxc-xlarge (lxc-size 16 16384))

(def default-pool :default)

(def local (node :localhost))

(defn lxc-volume
  "Add a LXC volume to an container"
  ([size]
   (lxc-volume size default-pool))
  ([size pool]
   (fn [{:keys [machine] :as container}]
     ((lxc-volume size pool (machine :hostname)) container)))
  ([size pool vname]
   (fn [container]
     (update-in container [:lxc :volumes]
                (fn [vs]
                  (conj vs {:device "vdb" :type "qcow2" :size size
                            :pool pool :clear true :name vname}))))))

; Default pool LXC volumes
(def ^{:doc "128Gb LXC volume"} lxc-vol-128G (lxc-volume 128))

(def ^{:doc "256Gb LXC volume"} lxc-vol-256G (lxc-volume 256))

(def ^{:doc "512Gb LXC volume"} lxc-vol-512G (lxc-volume 512))

(def ^{:doc "1TB LXC volume"} lxc-vol-1T (lxc-volume 1024))

(defn refer-lxc-presets []
  (require '[re-core.presets.lxc :as lxc :refer [lxc-tiny lxc-small lxc-medium lxc-large lxc-xlarge lxc-size lxc-volume]]))





