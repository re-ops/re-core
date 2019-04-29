(ns re-core.specs
  (:require
   [clojure.spec.alpha :as s]
   [re-share.spec :as re-ops]
   [re-core.presets.instance-types :as types]
   [re-core.model :refer (figure-virt)]))

(def digital-regions #{"nyc1" "nyc2" "nyc3" "tor1" "sfo1" "sfo2" "sgp1" "lon1"})

(def droplet-sizes #{"s-1vcpu-1gb" "s-1vcpu-2gb" "s-1vcpu-3gb" "s-2vcpu-2gb" "512mb"})

(s/def :digital/region (s/and string? digital-regions))

(s/def :digital/size (s/and string? droplet-sizes))

(s/def :digital/private_networking boolean?)

(s/def ::node (s/and keyword? #(re-matches #"\w+" (name %))))

(s/def ::lxc (s/keys :req-un [::node]))

(s/def ::kvm (s/keys :req-un [::node]))

(s/def ::digital-ocean (s/keys :req-un [:digital/region :digital/size :digital/private_networking]))

(defmulti system (fn [spec] (figure-virt spec)))

(defmethod system :lxc [_] (s/keys :req-un [:resource/machine ::lxc]))

(defmethod system :kvm [_] (s/keys :req-un [:resource/machine ::kvm]))

(defmethod system :digital-ocean [_] (s/keys :req-un [::digital-ocean]))

(def ip #"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")

(def hostname-regex #"([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])*")

(s/def ::hostname (s/and string? #(re-matches hostname-regex %)))

(def domain-regex #"([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])")

(s/def ::domain (s/and string? #(re-matches domain-regex %)))

(def user-regex #"^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\$)$")

(s/def ::user (s/and string? #(re-matches user-regex %)))

(def os-regex #"^[a-z]+\-[1-9]{1,2}\.[0-9]{1,2}[\.0-9]{0,2}$")

(s/def ::os (s/and keyword? #(re-matches os-regex (name %))))

(def valid-cpus (into #{} (map :cpu (vals types/all))))

(s/def ::cpu (s/and int? valid-cpus))

(def valid-ram (into #{} (map :ram (vals types/all))))

(s/def ::ram (s/and int? valid-ram))

(s/def :common/machine (s/keys :req-un [::hostname ::domain ::user ::os]))

(s/def :resource/machine (s/keys :req-un [::os ::cpu ::ram]))

(s/def ::system (s/merge (s/multi-spec system figure-virt) (s/keys :req-un [:common/machine])))

(comment
  (def redis-lxc
    {:machine {:hostname "red1" :user "root" :domain "local"
               :os :ubuntu-18.04.2 :cpu 4 :ram 1}
     :lxc {:node :localhost}
     :type "redis"})

  (def redis-kvm
    {:machine {:hostname "red1" :user "re-ops" :domain "local"
               :os :ubuntu-18.04.2 :cpu 4 :ram 1}
     :kvm {:node :localhost}
     :type "redis"})

  (def redis-digital
    {:machine {:hostname "red1" :user "root"
               :domain "local" :os :ubuntu-18.04.2}
     :digital-ocean {:region "lon1" :size "512mb"
                     :private_networking false}
     :type "redis"})

  (s/explain ::system redis-digital))
