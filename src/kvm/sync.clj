(ns kvm.sync
  "Sync kvm information info ES"
  (:require 
    [clojure.data.zip.xml :as zx]
    [es.systems :as s]
    [kvm.common :refer (get-domain domain-zip domain-list)]
    [kvm.connection :refer (with-connection c)]
    [re-core.model :refer (hypervisor*)]
    [re-core.core :refer (Sync)]))

(defn nodes []
  (hypervisor* :kvm :nodes))

(defn- cpu [root]
   (Integer/parseInt (first (zx/xml-> root :vcpu zx/text))))

(defn- ram [root]
   (/ (BigDecimal. (first (zx/xml-> root :memory zx/text))) 1024))

(defn- host
   [root]
  (first (zx/xml-> root :name zx/text)))

(defrecord KvmSync [] 
  Sync
  (scan [this]
    ) 

  (persist [this]
    
    ))


(comment
  (let [node {:user "ronen" :host "localhost" :port 22}]
    (with-connection
      (clojure.pprint/pprint (first (map (partial domain-zip (c)) (kvm.common/domain-list (c))))))))
