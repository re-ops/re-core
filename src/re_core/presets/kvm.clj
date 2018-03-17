(ns re-core.presets.kvm
  "KVM System presets"
  (:require
   [re-core.presets.common :refer (os machine)]
   [clojure.core.strint :refer (<<)]))

(defn kvm [machine node]
  {:machine machine :kvm {:node node}})

(defn kvm-machine [cpu ram os]
  (merge {:cpu cpu :ram ram} (machine "re-ops" "local" os)))

(def default-node :remote)

(defn node [n]
  (fn [instance]
    (assoc-in instance [:kvm :node] n)))

(defn kvm-size
  ([cpu ram]
   (kvm-size cpu ram :ubuntu-16.04))
  ([cpu ram os]
   (kvm (kvm-machine cpu ram os) default-node)))

; Default kvm instance types
(def #^{:doc "Tiny kvm instance"} kvm-tiny (kvm-size 1 512))

(def #^{:doc "Small kvm instance"} kvm-small (kvm-size 2 1024))

(def #^{:doc "Medium kvm instance"} kvm-medium (kvm-size 4 4096))

(def #^{:doc "Large kvm instance"} kvm-large (kvm-size 8 8192))

(def #^{:doc "Extra large kvm instance"} kvm-xlarge (kvm-size 16 16384))

(def default-pool :default)

(def localhost (node :localhost))

(defn kvm-volume
  "Add a kvm volume to an instance"
  ([size]
   (kvm-volume size default-pool))
  ([size pool]
   (fn [{:keys [machine] :as instance}]
     ((kvm-volume size pool (machine :hostname)) instance)))
  ([size pool vname]
   (fn [instance]
     (update-in instance [:kvm :volumes]
                (fn [vs]
                  (conj vs {:device "vdb" :type "qcow2" :size size
                            :pool pool :clear true :name vname}))))))

; Default pool kvm volumes
(def #^{:doc "128Gb kvm volume"} vol-128G (kvm-volume 128))

(def #^{:doc "256Gb kvm volume"} vol-256G (kvm-volume 256))

(def #^{:doc "512Gb kvm volume"} vol-512G (kvm-volume 512))

(def #^{:doc "1TB kvm volume"} vol-1T (kvm-volume 1024))

(defn refer-kvm-presets []
  (require '[re-core.presets.kvm :as ksp :refer [kvm-tiny kvm-small kvm-medium kvm-large kvm-xlarge vol-128G
                                                 vol-256G vol-512G vol-1T kvm-size kvm-volume node localhost]]))
