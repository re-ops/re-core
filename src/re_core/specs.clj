(ns re-core.specs
  (:require
   [me.raynes.fs :as fs]
   [re-core.persistency.types :as es]
   [clojure.spec.alpha :as s]
   [re-core.presets.instance-types :as types]
   [re-core.model :refer (figure-virt)]))

(defn alpha? [s]
  (re-matches #"\w+" s))

; Digital ocean
(def digital-regions #{"nyc1" "nyc2" "nyc3" "tor1" "sfo1" "sfo2" "sgp1" "lon1"})

(def droplet-sizes (into #{} (map :size (vals types/slugs))))

(s/def :digital/region (s/and string? digital-regions))

(s/def :digital/size (s/and string? droplet-sizes))

(s/def :digital/private_networking boolean?)

; Networking properties
(defn ethernet? [s]
  (re-matches #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$" s))

(s/def ::mac (s/and string? ethernet?))

(defn ip? [s]
  (and (not (nil? s)) (re-matches #"^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$" s)))

(s/def ::ip (s/and string? ip?))

(s/def ::broadcast (s/and string? ip?))

(defn hostname? [s]
  (re-matches #"([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]|[\\.])*" s))

(s/def ::hostname (s/and string? hostname?))

(defn domain? [s]
  (re-matches #"([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*(\.)?[A-Za-z0-9]*)" s))

(s/def ::domain (s/and string? domain?))

; Common properties
(s/def ::node (s/and keyword? (comp alpha? name)))

(defn user? [s]
  (re-matches #"^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\$)$" s))

(s/def ::user (s/and string? user?))

(defn os? [s]
  (re-matches #"^[a-z]+\-([a-z]+\-)*[0-9]{1,2}\.[0-9]{1,2}[\.0-9]{0,2}.*" s))

(s/def ::os (s/and keyword? (comp os? name)))

(def valid-cpus (into #{} (map :cpu (vals types/all))))

(s/def ::cpu (s/and int? valid-cpus))

(def valid-ram (into #{} (map :ram (vals types/all))))

(s/def ::ram (s/and number? valid-ram))

; Hypervisors

(s/def ::lxc (s/keys :req-un [::node]))

(s/def ::kvm (s/keys :req-un [::node]))

(s/def ::digital-ocean (s/keys :req-un [:digital/region :digital/size :digital/private_networking]))

(s/def ::physical (s/keys :req-un [::mac ::broadcast]))

(defmulti system (fn [spec] (figure-virt spec)))

(defmethod system :lxc [_] (s/keys :req-un [:resource/machine ::lxc]))

(defmethod system :physical [_] (s/keys :req-un [::physical]))

(defmethod system :kvm [_] (s/keys :req-un [:resource/machine ::kvm]))

(defmethod system :digital-ocean [_] (s/keys :req-un [::digital-ocean]))

; Common and main specs
(s/def :common/machine (s/keys :req-un [::hostname ::domain ::user ::os] :opt-un [::ip]))

(s/def :system/type (s/and keyword? (comp alpha? name) es/exists?))

(s/def :system/description string?)

(s/def :resource/machine (s/keys :req-un [::os ::cpu ::ram]))

(s/def ::system (s/merge (s/multi-spec system figure-virt) (s/keys :req-un [:common/machine :system/type :system/description])))

; Type specs
(s/def :type/args (s/* string?))

(defn plan-exists? [p]
  (resolve p))

(s/def :type/plan (s/and symbol? plan-exists?))

(s/def :type/type string?)

(s/def :type/description string?)

(s/def :type/src (s/and string? fs/exists?))

(s/def ::cog (s/keys :req-un [:type/args :type/plan :type/src]))

(s/def ::type (s/keys :req-un [::cog :type/description :type/type]))
