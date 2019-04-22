(ns re-core.specs
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]
   [re-core.model :refer (figure-virt)]))

(s/def ::node keyword?)

(defmulti system (fn [spec] (figure-virt spec)))

(defmethod system :lxc [_] (s/keys :req-un [::node]))

(s/def ::system (s/multi-spec system figure-virt))

(comment
  (s/explain ::system {:lxc {}}))
