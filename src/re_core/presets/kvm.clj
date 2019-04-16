(ns re-core.presets.kvm
  "KVM System presets")

(def default-pool :default)

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
(def ^{:doc "128Gb kvm volume"} vol-128G (kvm-volume 128))

(def ^{:doc "256Gb kvm volume"} vol-256G (kvm-volume 256))

(def ^{:doc "512Gb kvm volume"} vol-512G (kvm-volume 512))

(def ^{:doc "1TB kvm volume"} vol-1T (kvm-volume 1024))

(defn refer-kvm-presets []
  (require '[re-core.presets.kvm :as ksp :refer [vol-128G vol-256G vol-512G vol-1T kvm-volume]]))

