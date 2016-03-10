(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

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
