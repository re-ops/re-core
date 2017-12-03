(ns kvm.clone
  "see clone https://github.com/xebialabs/overcast/blob/master/src/main/java/com/xebialabs/overcast/support/libvirt/DomainWrapper.java"
  (:require
   [kvm.volumes :refer (volume-xml into-volume get-disks update-disks)]
   [kvm.common :refer (connect tree-edit domain-zip state)]
   [clojure.zip :as zip]
   [clojure.data.zip.xml :as zx]
   [clojure.data.xml :as xml])
  (:import
   java.text.SimpleDateFormat
   org.libvirt.Connect))

(defn version
  [c]
  (.getLibVirVersion c))

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
    (or (= tag :memory) (= tag :currentMemory))))

(defn set-ram [xml ram]
  (zip/xml-zip
   (tree-edit xml ram? (fn [node] (assoc node :content (list (* ram 1024)))))))

(defn clone-root [root name cpu ram]
  (-> root
      (set-name name) clear-uuid clear-all-macs (set-cpu cpu) (set-ram ram)))

(defn clone-volume-xml
  "A cloned volume XML"
  [{:keys [volume type file]} name']
  (volume-xml (.capacity (.getInfo volume)) "B" type file name' true))

(defn clone-name [name' idx]
  (str name' "-" (str idx) ".qcow2"))

(defn clone-disks [c name' root]
  (let [volumes (map-indexed vector (map (partial into-volume c) (get-disks root)))]
    (doall
     (for [[idx {:keys [volume] :as v}] volumes :let [pool (.storagePoolLookupByVolume volume) new-name (clone-name name' idx)]]
       (assoc v :volume (.storageVolCreateXML pool (xml/emit-str (clone-volume-xml v new-name)) 0))))))

(defn clone-domain [c id {:keys [name cpu ram] :as target}]
  (let [root (domain-zip c id) volumes (clone-disks c name root)
        target-root (update-disks (clone-root root name cpu ram) volumes)
        cloned-domain (.domainDefineXML c (xml/emit-str target-root))]
    (.create cloned-domain)))
