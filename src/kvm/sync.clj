(ns kvm.sync
  "Synching kvm status into re-core"
  (:refer-clojure :exclude [read-string])
  (:require
   [re-core.model :refer (hypervisor)]
   [clojure.edn :refer (read-string)]
   [clojure.data.zip.xml :as zx]
   [kvm.common :refer (domain-list get-domain connect domain-zip)]))

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

(defn descriptive-domains
  "Domains that have a description string set (we can import them)"
  [c]
  (filter
   (fn [d] (not (empty? (description-meta c d))))
   (domain-list c)))

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

(defn find-template
  "find os key from template"
  [t]
  {:post [(not (nil? %))]}
  (first
   (first
    (filter (fn [[_ {:keys [template]}]] (= template t)) (hypervisor :kvm :ostemplates)))))

(defn into-system
  "Convert domain into a system"
  [c d]
  (let [{:keys [os user node type]} (description-meta c d) root (domain-zip c d)]
    {:machine {:hostname (hostname root) :user user :os (find-template os)
               :cpu (Integer/parseInt (vcpu root)) :ram (/ (memory root) 1024)}
     :kvm {:node node}
     :type type}))

