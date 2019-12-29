(ns kvm.volumes
  (:require
   [taoensso.timbre :as timbre]
   [clojure.core.strint :refer (<<)]
   [clojure.zip :as zip]
   [kvm.common :refer (tree-edit domain-zip get-domain)]
   [clojure.data.xml :as xml :refer (element)]
   [clojure.data.zip.xml :as zx]))

(timbre/refer-timbre)

(defmacro safe
  "Volumes might be destroyed as we iterate though, its safe to ignore"
  [f]
  `(try
     ~f
     (catch org.libvirt.LibvirtException e#
       (when-let [m# (-> e# Throwable->map :via :message)]
         (when-not (.contains m# "Storage volume not found")
           (throw e#))))))

(defn volumes [pool]
  (filter identity
          (map
           (fn [v] (safe (.storageVolLookupByName pool v)))
           (.listVolumes pool))))

(defn pool-lookup [c id]
  (.storagePoolLookupByName c id))

(defn pools [c]
  (map (partial pool-lookup c) (.listStoragePools c)))

(defn find-volume [c path]
  (first
   (filter #(= (safe (.getPath %)) path)
           (mapcat (fn [pool] (volumes pool)) (pools c)))))

(defn get-disks [root]
  (map vector
       (zx/xml-> root :devices :disk :target (zx/attr :dev))
       (zx/xml-> root :devices :disk :source (zx/attr :file))
       (zx/xml-> root :devices :disk :driver (zx/attr= :name "qemu") (zx/attr :type))))

(defn into-volume [c [dev file type]]
  (let [volume (find-volume c file)]
    (when (nil? volume)
      (throw (ex-info "volume not found please check that the pool is started" {:dev dev :file file :type type})))
    {:device dev :file file :type type :volume volume}))

(defn volume-xml
  "A libvirt volume XML"
  ([size file name']
   (volume-xml size "B" file name' false))
  ([size unit type' file name' & block?]
   (element :volume {}
            (element :name {} name')
            (element :allocation {:unit unit} 0)
            (element :capacity {:unit unit} size)
            (when (first block?) (element :backingStore {}
                                          (element :path {} file)
                                          (element :format {:type type'} nil)))
            (element :target {}
                     (element :format {:type type'} nil)
                     (element :compat {} "1.1")))))

(defn volume-disk-xml
  "A libvirt disk volume XML"
  [file device]
  (element :disk {:type "file" :device "disk"}
           (element :driver {:name "qemu" :type "qcow2"})
           (element :source {:file file})
           (element :target {:dev device :bus "virtio"})))

(defn attach
  "Attach a image file to domain using device"
  [domain image device]
  (.attachDevice domain (xml/emit-str (volume-disk-xml image device))))

(defn list-volumes
  [c domain]
  (reverse (map (partial into-volume c) (get-disks (domain-zip c domain)))))

(defn exists? [c domain image]
  (let [existing (list-volumes c domain)
        found (first (filter (fn [{:keys [file]}] (= file image)) existing))]
    (when found true)))

(defn create-volume
  "Create a volume on pool with given capacity"
  [c domain {:keys [id path]} type capacity image device]
  (let [volume (xml/emit-str (volume-xml capacity "G" type path image))]
    (when (not (exists? c domain image))
      (.storageVolCreateXML (pool-lookup c id) volume 0)
      (attach (get-domain c domain) (<< "~{path}~{image}") device))))

(defn create-volumes [c domain volumes]
  (doseq [{:keys [device type size pool name] :as v} volumes]
    (create-volume c domain pool type size name device)))

(defn clear-volumes [c domain volumes]
  (doseq [{:keys [volume file]} (list-volumes c domain)]
    (.delete volume 0))
  (doseq [{:keys [pool name]} volumes]
    (when-let [volume (find-volume c (<< "~(:path pool)~{name}"))]
      (.delete volume 0))))

(defn disk? [loc]
  (= :disk (:tag (zip/node loc))))

(defn update-file [volumes node]
  (let [target (first (filter (fn [element] (= :target (:tag element))) (:content node)))
        {:keys [volume]} (first (filter (fn [{:keys [device]}] (= (get-in target [:attrs :dev]) device)) volumes))]
    (assoc node :content
           (map
            (fn [{:keys [tag attrs] :as element}]
              (if (= tag :source) (assoc element :attrs (assoc attrs :file (.getPath volume)))  element)) (:content node)))))

(defn update-disks [root volumes]
  (zip/xml-zip (tree-edit root disk? (partial update-file volumes))))

