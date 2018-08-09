(ns re-core.presets.digitial
  "Digitalocean presets"
  (:require
   [re-core.common :refer (hostname)]
   [re-core.presets.common :as c]))

(defn region [r]
  (fn [instance]
    (assoc-in instance [:digital-ocean :region] r)))

(defn size
  "instance size"
  [s]
  (fn [instance]
    (assoc-in instance [:digital-ocean :size] s)))

(defn private [t]
  (fn [instance]
    (assoc-in instance [:digital-ocean :private_networking] t)))

(def nyc1 (region "nyc1"))

(def nyc2 (region "nyc2"))

(def nyc3 (region "nyc3"))

(def tor1 (region "tor1"))

(def sfo1 (region "sfo1"))

(def sfo2 (region "sfo2"))

(def sgp1 (region "sgp1"))

(def lon1 (region "lon1"))

(defn digital-machine [os]
  (c/machine "re-ops" "local" os))

(defn droplet
  ([size]
   (droplet size :ubuntu-16.04))
  ([size os]
   {:machine (digital-machine os)
    :digital-ocean {:region "sfo1" :size size
                    :private_networking false}}))

(def vcpu-1-1G (droplet "s-1vcpu-1gb"))

(def vcpu-1-2G (droplet "s-1vcpu-2gb"))

(def vcpu-1-3G (droplet "s-1vcpu-3gb"))

(def vcpu-2-2G (droplet "s-2vcpu-2gb"))

(defn refer-digital-presets []
  (require '[re-core.presets.digitial :as dig :refer [
        vcpu-1-1G vcpu-1-2G vcpu-1-3G vcpu-2-2G
        sgp1 lon1 nyc1 nyc2 nyc3 tor1 sfo1 sfo2
        ]]))
