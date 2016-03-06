(ns kvm.common
 (:require [clojure.zip :as zip])
 (:import 
    java.text.SimpleDateFormat
    org.libvirt.Connect))

(defn connect 
   "Connecting" 
   [uri]
   (Connect. uri))

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

(defn domain-list
   [c]
   (map (partial get-domain c) (.listDefinedDomains c)))

(defn state [domain]
  (let [s (.toString (.state (.getInfo domain)))]
    (.toLowerCase (.replace s "VIR_DOMAIN_" ""))))
