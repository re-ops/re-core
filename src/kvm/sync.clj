(ns kvm.sync
  "Synching kvm status into re-core"
  (:refer-clojure :exclude [read-string])
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
  [c domain]
  (first
   (zx/xml-> (domain-zip c domain) :description zx/text read-string)))

(defn vcpu
  "read cpu"
  [root]
  (first (zx/xml-> root :vcpu zx/text)))

(defn hostname
  [root]
  (first (zx/xml-> root :name zx/text)))

(defn memory
  "read ram"
  [root]
  (Integer/parseInt (first (zx/xml-> root :memory zx/text))))

(defn into-system
  "Convert domain into a system"
  [c d]
  (let [{:keys [os user node type]} (description-meta c d) root (domain-zip c d)]
    {:machine {:hostname (hostname root) :user user :os os
               :cpu (vcpu root) :memory (/ (memory root) 1024)}
     :kvm {:node node}
     :type type}))

