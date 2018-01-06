(ns re-core.preset
  "VM presets"
  (:require [re-core.common :refer (gen-uuid)]))

(defn name-gen
  "Generating a unique hostname from type + uuid"
  [instance]
  (assoc-in instance [:machine :hostname]
            (str type "-" (.substring (gen-uuid) 0 10))))

(defn machine [user domain os] {:user user :domain domain :os os})

(defn kvm
  ([machine node]
   (kvm machine node []))
  ([machine node volumes] {:machine machine
                           :kvm {:node node}
                           :volumes volumes}))

(defn kvm-machine [cpu ram]
  (merge {:cpu cpu :ram ram} (machine "re-ops" "local" :ubuntu-16.04)))

(def default-node :remote)

(defn kvm-size
  [cpu ram]
  (kvm (kvm-machine cpu ram) default-node))

; Default kvm instance types
(def #^{:doc "Tiny kvm instance"} kvm-tiny (kvm-size 1 512))

(def #^{:doc "Small kvm instance"} kvm-small (kvm-size 2 1024))

(def #^{:doc "Medium kvm instance"} kvm-medium (kvm-size 4 4096))

(def #^{:doc "Large kvm instance"} kvm-large (kvm-size 8 8192))

(def #^{:doc "Extra large kvm instance"} kvm-xlarge (kvm-size 16 16384))

(def default-pool :default)

(defn kvm-volume
  "Add a kvm volume to an instance"
  ([size]
   (kvm-volume size default-pool))
  ([size pool]
   (fn [{:keys [machine] :as instance}]
     (update-in instance [:kvm :volumes]
                (fn [vs]
                  (conj vs {:device "vdb" :type "qcow2" :size size
                            :pool pool :clear true :name (machine :hostname)}))))))

; Default pool kvm volumes
(def #^{:doc "128Gb kvm volume"} vol-128G (kvm-volume 128))

(def #^{:doc "256Gb kvm volume"} vol-256G (kvm-volume 256))

(def #^{:doc "512Gb kvm volume"} vol-512G (kvm-volume 512))

(def #^{:doc "1TB kvm volume"} vol-1T (kvm-volume 1024))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args) {:keys [fns]} m]
      (cond
        (string? a) (into-spec (assoc m :hostname a) (rest args))
        (number? a) (into-spec (assoc m :count a) (rest args))
        (keyword? a) (into-spec (assoc m :type a) (rest args))
        (fn? a) (into-spec (assoc m :fns (conj fns a)) (rest args))))))

(defn refer-presets []
  (require '[re-core.preset :as pres :refer [kvm-tiny kvm-small kvm-medium kvm-large kvm-xlarge
                                             vol-128G vol-256G vol-512G vol-1T]]))

