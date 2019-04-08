(ns lxc.spec
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]))

(def p12-reg #"\\w+.p12")

(s/def :lxc/p12 #(re-matches p12-reg %))

(def crt-reg #"\\w+.crt")

(s/def :lxc/crt #(re-matches crt-reg %))

(s/def :lxc/node
  (s/keys :re-un [:re-ops/host :re-ops/port :re-ops/path :lxc/p12 :re-ops/password :lxc/crt]))

(s/def :lxc/container
  (s/keys :re-un [:re-ops/node]))
