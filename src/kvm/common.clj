(ns kvm.common
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.data.xml :as xml]
   [clojure.zip :as zip])
  (:import
   java.text.SimpleDateFormat
   org.libvirt.Connect))

(refer-timbre)

(defn connect
  "Connecting"
  ([uri]
   (try
     (Connect. uri)
     (catch Exception e
       (error "Failed to connect to" uri)))))

(defn tree-edit
  "Take a zipper, a function that matches a pattern in the tree,
   and a function that edits the current location in the tree.  Examine the tree
   nodes in depth-first order, determine whether the matcher matches, and if so
   apply the editor."
  [zipper matcher editor]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if-let [matcher-result (matcher loc)]
        (let [new-loc (zip/edit loc editor)]
          (if (not (= (zip/node new-loc) (zip/node loc)))
            (recur (zip/next new-loc))))
        (recur (zip/next loc))))))

(defn get-domain [c name]
  (.domainLookupByName c name))

(defn domain-list [c]
  (let [defined (into [] (.listDefinedDomains c))
        active (into []  (map #(.getName (.domainLookupByID c %)) (.listDomains c)))]
    (into defined active)))

(defn state [domain]
  (let [s (.toString (.state (.getInfo domain)))]
    (.toLowerCase (.replace s "VIR_DOMAIN_" ""))))

(defn xml-desc [domain]
  (.getXMLDesc domain 0))

(defn parse [s]
  (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn domain-zip [c id]
  (-> (get-domain c id) xml-desc parse))

