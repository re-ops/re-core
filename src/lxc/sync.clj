(ns lxc.sync
  (:require
   (lxc.client :as lxc)))

(def redis-lxc
  {:machine {:hostname "red1" :user "root" :domain "local"
             :os :ubuntu-18.04.2 :cpu 4 :ram 1}
   :lxc {:node :localhost}
   :type :redis})

(defn into-system [name]
  (map lxc/state {:name name}))

(defn sync-node [node]
  (map into-system (lxc/into-names (lxc/list node))))

(comment
  (require '[re-core.model :refer (hypervisor)])

  (def node
    (merge {:host "127.0.0.1" :port "8443"} (hypervisor :lxc :auth))))
