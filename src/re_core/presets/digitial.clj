(ns re-core.presets.digitial
  "Digitalocean presets"
  (:require
   [re-core.common :refer (hostname)]
   [re-core.presets.systems :as sp]))

(defn region [r]
  (fn [instance]
    (assoc-in instance [:digital-ocean :region] r)))

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

(defn refer-digital-presets []
  (require '[re-core.presets.digitial :as dig :refer [sgp1 lon1 nyc1 nyc2 nyc3 tor1 sfo1 sfo2]]))
