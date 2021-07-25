(ns re-flow.nebula
  "Nebula cert deployment"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.certs :refer (refer-certs)]
   [re-flow.actions :refer (run)]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets materialize-preset)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e failure? successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)
(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::uploaded :re-flow.core/state)
(derive ::sign :re-flow.core/state)
(derive ::signed :re-flow.core/state)
(derive ::downloaded :re-flow.core/state)

(s/def ::dest string?)

(s/def ::certs string?)

(s/def ::group string?)

(s/def ::hostname string?)

(s/def ::intermediary string?)

(s/def ::groups (s/coll-of ::group))

(s/def ::host (s/keys :req-un [::hostname ::groups]))

(s/def ::range :re-core.specs/ip)

(s/def ::hosts (s/coll-of ::host))

(s/def ::nebula
  (s/keys :req-un [::certs ::dest ::range ::hosts ::intermediary]))

(def instance
  {:base kvm :args [defaults local small :nebula "Nebula signing instance"]})

(defrule check
  "Check that the fact is matching the ::certs spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::nebula ?e))]
    (info (expound/expound-str ::nebula ?e))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate cert signing spec")))))

(defrule create
  "Triggering the creation of the signing instance"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup sign instance")
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance)))

(defrule upload-cert
  "Upload sign cert and key"
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (info "uploading cert and key to signing host")
  (let [r1 (run :upload ?e (?e :dest) (<< "~(?e :certs)/ca.crt"))
        r2 (run :upload ?e (?e :dest) (<< "~(?e :certs)/ca.key"))]
    (insert! (assoc ?e :state ::uploaded :failure (or (failure? ?e r1) (failure? ?e r2))))))

(defn ip-range
  "A simplistic ip range generator we assume our range is x.x.x.0 we generate the 256 ips in that range"
  [r]
  (let [prefix (clojure.string/join "." (butlast (clojure.string/split r #"\.")))]
    (map (fn [i] (<< "~{prefix}.~{i}/24")) (range 1 256))))

(defrule signup
  "Sign host keys"
  [?e <- ::uploaded [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (info "Signing keys")
  (let [crt (<< "~(?e :dest)/ca.crt")
        key (<< "~(?e :dest)/ca.key")
        signups (map (fn [m ip] (assoc m :ip ip)) (?e :hosts) (ip-range (?e :range)))]
    (doseq [{:keys [hostname ip groups]} signups]
      (let [r (run ::sign ?e hostname ip groups crt key (?e :dest))]
        (insert! (assoc ?e :state ::signed :failure (failure? ?e r) :hostname hostname))))))

(defrule download-nebula-keys
  "Download keys for host"
  [?e <- ::signed [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (info "downloading certs for" (?e :hostname))
  (let [{:keys [intermediary hostname]} ?e
        inter-target (<< "~{intermediary}/~{hostname}")
        m1 (run :mkdir ?e intermediary)
        m2 (run :mkdir ?e inter-target)
        d1 (run :download ?e (<< "~(?e :dest)/~{hostname}.key") inter-target)
        d2 (run :download ?e (<< "~(?e :dest)/~{hostname}.crt") inter-target)
        failure (or (failure? ?e d1) (failure? ?e d2) (not m1) (not m2))]
    (info (failure? ?e d1) (failure? ?e d2) (not m1) (not m2))
    (insert! (assoc ?e :state ::downloaded :failure failure))))

(defrule distribute
  "Fetch signed certs and distribute them to the hosts"
  [?e <- ::downloaded [{:keys [flow failure]}] (= failure false)]
  =>
  (info "distributing certs to host" (?e :hostname)))
