(ns re-share.config.spec
  "Re-ops configuration file spec"
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]))

; Shared
(s/def ::password
  ; Minimum eight characters, at least one letter, one number and one special character:
  (s/and string? #(re-matches #"^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$" %)))

(s/def ::index string?)

(s/def ::elasticsearch (s/keys :req-un [::index]))

(s/def ::hosts (s/coll-of :re-ops/host))

(s/def ::cluster (s/keys :req-un [::hosts]))

(s/def :shared/elasticsearch (s/map-of keyword? ::cluster))

(s/def :shared/private-key-path string?)

(s/def :shared/public string?)

(s/def :shared/ssh (s/keys :req-un [:shared/private-key-path]))

(s/def :shared/pgp (s/keys :req-un [:shared/public]))

(defn email? [s]
  (re-matches #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" s))

(s/def :shared/to (s/and string? email?))

(s/def :shared/from (s/and string? email?))

(s/def :shared/email (s/keys :req-un [:shared/to :shared/from]))

(s/def ::shared (s/keys :req-un [:shared/elasticsearch :shared/ssh :shared/pgp :shared/email]))

; Re-mote
(s/def ::re-mote (s/keys :req-un [::elasticsearch]))

; Re-core
(s/def ::username string?)

(s/def :kvm/node (s/keys :req-un [::username :re-ops/host :re-ops/port]))

(s/def :kvm/nodes (s/map-of keyword? :kvm/node))

(s/def :re-core/kvm (s/keys :req-un [:kvm/nodes]))

(s/def :lxc/crt #(re-matches #"\w+\.crt" %))

(s/def :lxc/p12
  (s/and string? #(re-matches #"\w+\.p12$" %)))

(s/def :lxc/auth (s/keys :req-un [::password :re-ops/path :lxc/p12]))

(s/def :lxc/node (s/keys :req-un [:re-ops/host :re-ops/port]))

(s/def :lxc/nodes (s/map-of keyword? :lxc/node))

(s/def :re-core/lxc (s/keys :req-un [:lxc/nodes :lxc/auth]))

(s/def :re-core/hypervisor (s/keys :opt-un [:re-core/kvm :re-core/lxc]))

(s/def :re-core/queue-dir string?)

(s/def ::re-core (s/keys :req-un [::elasticsearch :re-core/hypervisor :re-core/queue-dir]))

; Combined
(s/def ::config (s/keys :req-un [::re-mote ::re-core ::shared]))

