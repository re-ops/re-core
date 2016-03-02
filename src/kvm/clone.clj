(ns kvm.clone
  (:import 
    java.text.SimpleDateFormat
    org.libvirt.Connect) 
  (:require 
     [kvm.disks :refer (get-disks find-volume clone-disks update-disks)]
     [kvm.common :refer (connect tree-edit)]
     [clojure.pprint :refer (pprint)]
     [clojure.zip :as zip]
     [clojure.data.zip.xml :as zx]
     [clojure.data.xml :as xml]))

(defn version 
   [c]
   (.getLibVirVersion c))

(defn get-domain [c name] 
   (.domainLookupByName c name))

(defn domain-list
   [c]
   (map (partial get-domain c) (.listDefinedDomains c)))

(defn state [domain]
  (.state (.getInfo domain)))

(defn xml-desc [domain]
  (.getXMLDesc domain 0))

(defn parse [s]
   (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn set-name [xml name']
  (-> xml zip/down zip/down (zip/edit (fn [e]  name')) zip/root zip/xml-zip))

(defn clear-uuid [xml]
  (-> xml zip/down zip/right zip/remove zip/root zip/xml-zip))

(defn interface? [loc]
   (= :interface (:tag (zip/node loc))))

(defn clear-mac [node]
  (assoc node :content (rest (:content node))))

(defn clear-all-macs [xml]
  (zip/xml-zip (tree-edit xml interface? clear-mac)))

;; (def connection (connect "qemu+ssh://ronen@localhost/system"))

(defn domain-zip [c id]
  (-> (get-domain c id) xml-desc parse))  

(defn clone-domain [c id name]
  (let [root (domain-zip c id) volumes (clone-disks c name root)
        cloned-root (-> root (set-name name) clear-uuid clear-all-macs (update-disks volumes))
        cloned-domain (.domainDefineXML c (xml/emit-str cloned-root))
        ]
    (.create cloned-domain)))

;; (clone-domain connection "ubuntu-15.04"  "dar")
;; (doall (map #(vector (.getID %) (.getName %)) (domain-list connection)))
; see clone https://github.com/xebialabs/overcast/blob/master/src/main/java/com/xebialabs/overcast/support/libvirt/DomainWrapper.java

