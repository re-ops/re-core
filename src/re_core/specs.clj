(ns re-core.specs
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]
   [re-core.model :refer (figure-virt)]))

(s/def ::node keyword?)

(s/def ::lxc (s/keys :req-un [::node]))

(defmulti system (fn [spec] (figure-virt spec)))

(defmethod system :lxc [_] (s/keys :req-un [::lxc]))

(s/def ::system (s/multi-spec system figure-virt))

(comment
  (def redis-lxc
    {:env :dev
     :owner "ronen"
     :machine {:hostname "red1" :user "root" :domain "local"
               :os :ubuntu-18.04.2 :cpu 4 :ram 1}
     :lxc {:node :localhost}
     :type "redis"})

  (s/explain ::system redis-lxc))
