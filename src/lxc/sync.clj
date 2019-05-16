(ns lxc.sync
  (:require
   [re-core.presets.common :as sp :refer (validate)]
   [clojure.data.json :as json]
   [lxc.client :as lxc]))

(defn into-system [node name]
  {:post [#(validate %)]}
  (json/read-str (get-in (lxc/get node {:name name}) [:metadata :description]) :key-fn keyword))

(defn sync-node [node]
  (map (partial into-system node) (lxc/into-names (lxc/list node))))
