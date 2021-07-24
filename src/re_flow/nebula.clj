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
   [re-flow.common :refer (run-?e run-?e-non-block results failure? successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)
(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::uploaded :re-flow.core/state)
(derive ::sign :re-flow.core/state)

(s/def ::dest string?)
(s/def ::crt string?)
(s/def ::key string?)
(s/def ::group string?)
(s/def ::name string?)
(s/def ::groups (s/coll-of ::group))
(s/def ::host (s/keys :req-un [::name ::groups]))
(s/def ::range :re-core.specs/ip)
(s/def ::hosts (s/coll-of ::host))

(s/def ::nebula
  (s/keys :req-un [::key ::crt ::dest]))

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
  (let [r1 (run ::upload ?e (?e :dest) (?e :key))
        r2 (run ::upload ?e (?e :dest) (?e :crt))]
    (insert! (assoc ?e :state ::uploaded :failure (or (failure? r1 ?e) (failure? r2 ?e))))))

(defn ip-range
  "A simplistic ip range generator we assume our range is x.x.x.0 we generate the 256 ips in that range"
  [r]
  (let [prefix (clojure.string/join "." (butlast (clojure.string/split r #"\.")))]
    (map (fn [i] (<< "~{prefix}.~{i}")) (range 1 256))))

(defrule signup
  "sign host keys"
  [?e <- ::uploaded [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (info "Signing keys")
  (let [signups (map (fn [m ip] (assoc m :ip ip)) (?e :hosts) (ip-range (?e :range)))
        results (map (fn [{:keys [name ip groups]}] (run ::sign ?e name ip groups)) signups)]
    (doseq [r results]
      (info r))))
