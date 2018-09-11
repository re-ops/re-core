(ns kvm.syncher
  "Synching kvm status into re-core"
  (:refer-clojure :exclude [sync read-string])
  (:require
   [clojure.edn :refer (read-string)]
   [clojure.zip :as zip]
   [clojure.data.zip.xml :as zx]
   [kvm.common :refer (domain-list get-domain connect domain-zip)]
   [re-core.core :refer (Sync)]))

(defn active-domains
  [c]
  (filter
   (fn [d] (= 1 (.isActive (get-domain c d))))
   (domain-list c)))

(defn description-meta
  "Grab domain metadata stored in the description string"
  [domain]
  (first
   (zx/xml-> (domain-zip c domain) :description zx/text read-string)))

(defn into-system
  "Convert domain into a system"
  [domain])

(defrecord Libsync []
  Sync
  (sync [this]))

(comment
  (let [c (connect "qemu+ssh://ronen@localhost:22/system")]
    (let [root (first (map (partial domain-zip c) (active-domains c)))])))
