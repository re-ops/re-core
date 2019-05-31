(ns lxc.spec
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]))

(s/def :lxc/node
  (s/keys :re-un [:re-ops/host :re-ops/port :re-ops/path :lxc/p12 :re-ops/password :lxc/crt]))

(s/def :lxc/container
  (s/keys :re-un [:re-ops/node]))
