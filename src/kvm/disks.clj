(ns kvm.disks
  (:require 
    [clojure.zip :as zip]
    [kvm.common :refer (tree-edit)]
    [clojure.data.xml :as xml :refer (element)]
    [clojure.data.zip.xml :as zx])
  )

(defn volumes [c pool path]
  (map (fn [v] (.storageVolLookupByName pool v)) (.listVolumes pool)))

(defn find-volume [c path] 
  (let [pools (map #(.storagePoolLookupByName c %) (.listStoragePools c)) ]
    (first (filter #(= (.getPath %) path) (mapcat (fn [pool] (volumes c pool path))  pools)))
   ))

(defn get-disks [root]
  (map vector 
    (zx/xml-> root :devices :disk :target (zx/attr :dev))
    (zx/xml-> root :devices :disk :source (zx/attr :file))
    (zx/xml-> root :devices :disk :driver (zx/attr= :name "qemu") (zx/attr :type))))

(defn into-volume [c [dev file type]] 
  {:device dev :file file :type type :volume (find-volume c file)})

(defn clone-volume-xml [{:keys [volume type file] } name]
  (element :volume {} 
    (element :name {} name)
    (element :allocation {} "0")
    (element :capacity {} (.capacity (.getInfo volume)))
    (element :target {}
      (element :format {:type type} nil)       
      (element :compat {} "1.1")) 
    (element :backingStore {} 
       (element :path {} file)      
       (element :format {:type type} nil))))

(defn clone-name [name idx]
  (str name "-" (str idx) ".qcow2"))

(defn clear-volumes [c root] 
   (doseq [{:keys [volume]} (map (partial into-volume c) (get-disks root))]
     (.delete volume 0)))

(defn clone-disks [c name root]
  (let [volumes  (map-indexed vector (map (partial into-volume c) (get-disks root)))]
    (doall 
      (for [[idx {:keys [volume] :as v}] volumes :let [pool (.storagePoolLookupByVolume volume) new-name (clone-name name idx) ]]
        (assoc v :volume (.storageVolCreateXML pool (xml/emit-str (clone-volume-xml v new-name)) 0))))))

(defn disk? [loc]
   (= :disk (:tag (zip/node loc))))

(defn update-file [volumes node]
     (let [target (first (filter (fn [element] (= :target (:tag element))) (:content node)))
           {:keys [volume]} (first (filter (fn [{:keys [device]}] (= (get-in target [:attrs :dev]) device)) volumes)) ]
        (assoc node :content 
          (map 
            (fn [{:keys [tag attrs] :as element}] 
              (if (= tag :source) (assoc element :attrs (assoc attrs :file (.getPath volume)))  element)) (:content node))))) 

(defn update-disks [root volumes]
   (zip/xml-zip (tree-edit root disk? (partial update-file volumes))))

