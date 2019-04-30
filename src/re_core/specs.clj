(ns re-core.specs
  (:require
   [clojure.core.strint :refer (<<)]
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]
   [re-core.presets.instance-types :as types]
   [re-core.model :refer (figure-virt)]))

; Digital ocean
(def digital-regions #{"nyc1" "nyc2" "nyc3" "tor1" "sfo1" "sfo2" "sgp1" "lon1"})

(def droplet-sizes #{"s-1vcpu-1gb" "s-1vcpu-2gb" "s-1vcpu-3gb" "s-2vcpu-2gb" "512mb"})

(s/def :digital/region (s/and string? digital-regions))

(s/def :digital/size (s/and string? droplet-sizes))

(s/def :digital/private_networking boolean?)

; AWS

(def instance-types (into #{} (map name (keys types/aws))))

(s/def :aws/instance-type (s/and string? instance-types))

(s/def :aws/key-name (s/and string? #(re-matches #"\w+" %)))

(defn region-suffix [r]
  (get {"cn-north-1" "cn" "cn-northwest-1" "cn"} r "com"))

(def ec2-regions
  ["us-east-2" "us-east-1" "us-west-1" "us-west-2" "ap-east-1" "ap-south-1" "ap-northeast-3"
   "ap-northeast-2" "ap-southeast-1" "ap-southeast-2" "ap-northeast-1" "ca-central-1" "cn-north-1"
   "cn-northwest-1" "eu-central-1" "eu-west-1" "eu-west-2" "eu-west-3" "eu-north-1" "sa-east-1"
   "us-gov-east-1" "us-gov-west-1"])

(def ec2-endpoints
  (into #{} (map (fn [r] (<< "ec2.~{r}.amazonaws.~(region-suffix r)")) ec2-regions)))

(s/def :aws/endpoint (s/and string? ec2-endpoints))

(s/def :aws/security-groups (s/coll-of (s/and string? #(re-matches #"\w+" %))))

(s/def :aws/ebs-optimized boolean?)

; Networking properties
(def ethernet-address #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")

(s/def ::mac (s/and string? #(re-matches ethernet-address %)))

(def ip #"^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

(s/def ::ip (s/and string? #(re-matches ip %)))

(s/def ::broadcast (s/and string? #(re-matches ip %)))

(def hostname-regex #"([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])*")

(s/def ::hostname (s/and string? #(re-matches hostname-regex %)))

(def domain-regex #"([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])")

(s/def ::domain (s/and string? #(re-matches domain-regex %)))

(def user-regex #"^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\$)$")

; Common properties
(s/def ::node (s/and keyword? #(re-matches #"\w+" (name %))))

(s/def ::user (s/and string? #(re-matches user-regex %)))

(def os-regex #"^[a-z]+\-[1-9]{1,2}\.[0-9]{1,2}[\.0-9]{0,2}$")

(s/def ::os (s/and keyword? #(re-matches os-regex (name %))))

(def valid-cpus (into #{} (map :cpu (vals types/all))))

(s/def ::cpu (s/and int? valid-cpus))

(def valid-ram (into #{} (map :ram (vals types/all))))

(s/def ::ram (s/and int? valid-ram))

; Hypervisors
(s/def ::lxc (s/keys :req-un [::node]))

(s/def ::kvm (s/keys :req-un [::node]))

(s/def ::digital-ocean (s/keys :req-un [:digital/region :digital/size :digital/private_networking]))

(s/def ::aws (s/keys :req-un [:aws/instance-type :aws/key-name :aws/endpoint :aws/security-groups :aws/ebs-optimized]))

(s/def ::physical (s/keys :req-un [::mac ::broadcast]))

(defmulti system (fn [spec] (figure-virt spec)))

(defmethod system :lxc [_] (s/keys :req-un [:resource/machine ::lxc]))

(defmethod system :aws [_] (s/keys :req-un [::aws]))

(defmethod system :physical [_] (s/keys :req-un [::physical]))

(defmethod system :kvm [_] (s/keys :req-un [:resource/machine ::kvm]))

(defmethod system :digital-ocean [_] (s/keys :req-un [::digital-ocean]))

; Common and main specs
(s/def :common/machine (s/keys :req-un [::hostname ::domain ::user ::os] :opt-un [::ip]))

(s/def :resource/machine (s/keys :req-un [::os ::cpu ::ram]))

(s/def ::system (s/merge (s/multi-spec system figure-virt) (s/keys :req-un [:common/machine])))
