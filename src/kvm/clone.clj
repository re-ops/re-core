(ns kvm.clone
  (:import 
    java.text.SimpleDateFormat
    org.libvirt.Connect) 
  (:require 
     [kvm.disks :refer (get-disks find-volume clone-disks update-disks)]
     [kvm.common :refer (connect tree-edit get-domain state)]
     [clojure.zip :as zip]
     [clojure.data.zip.xml :as zx]
     [clojure.data.xml :as xml]))

(defn version 
   [c]
   (.getLibVirVersion c))

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

(defn vcpu? [loc]
   (= :vcpu (:tag (zip/node loc))))

(defn set-cpu [xml cpu]
  (zip/xml-zip 
    (tree-edit xml vcpu? (fn [node] (assoc node :content (list cpu))))))

(defn ram? [loc]
  (let [tag (:tag (zip/node loc))]
    (or (= tag :memory ) (= tag :currentMemory))))

(defn set-ram [xml ram]
  (zip/xml-zip 
    (tree-edit xml ram? (fn [node] (assoc node :content (list (* ram 1024)))))))

(defn domain-zip [c id]
  (-> (get-domain c id) xml-desc parse))  

(defn clone-root [root name cpu ram] 
  (-> root 
    (set-name name) clear-uuid clear-all-macs (set-cpu cpu) (set-ram ram)))

; see clone https://github.com/xebialabs/overcast/blob/master/src/main/java/com/xebialabs/overcast/support/libvirt/DomainWrapper.java
(defn clone-domain [c id {:keys [name cpu ram] :as target}]
  (let [root (domain-zip c id) volumes (clone-disks c name root)
        target-root (update-disks (clone-root root name cpu ram) volumes)
        cloned-domain (.domainDefineXML c (xml/emit-str target-root))]
    (.create cloned-domain)))

;; (def connection (connect "qemu+ssh://ronen@localhost/system"))
;; (state (get-domain connection "redis-sandbox_ubuntu")) 
;; (clone-domain connection "ubuntu-15.04" {:name "dar" :cpu 2 :ram 2048})
;; (clojure.pprint/pprint (domain-zip connection "redis-sandbox_ubuntu"))

