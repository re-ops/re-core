(ns re-flow.certs
  "Lets encrypt cert renewal flow"
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

(refer-certs)
(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::domains-ready :re-flow.core/state)
(derive ::renewed :re-flow.core/state)
(derive ::downloaded :re-flow.core/state)
(derive ::delivered :re-flow.core/state)
(derive ::timedout :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(s/def ::user string?)

(s/def ::token string?)

(s/def ::domain :re-core.specs/hostname)

(s/def ::domains (s/coll-of ::domain))

(s/def ::destination string?)

(s/def ::delivery (s/keys :req-un [::destination ::domain]))

(s/def ::distribution (s/map-of :re-mote.spec/host ::delivery))

(s/def ::certs
  (s/keys :req-un [::domains ::user ::token ::distribution]))

(def instance
  {:base kvm :args [defaults local small :letsencrypt "letsencrypt cert generation and distribution"]})

(defrule check
  "Check that the fact is matching the ::certs spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::certs ?e))]
    (info (expound/expound-str ::certs ?e))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate cert renewl spec")))))

(defrule create
  "Triggering the creation of the instance"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup certs instance")
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance)))

(defrule domains
  "Setup the domains we will generate certs for"
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (let [r (run ::set-domain ?e)]
    (insert!
     (-> ?e
         (dissoc :message :failure)
         (assoc  :state ::domains-ready :failure (failure? r ?e))))))

(defrule run-renewal
  "Renew the certificates"
  [?e <- ::domains-ready [{:keys [failure]}] (= failure false)]
  =>
  (let [r (run ::renew ?e (?e :user) (?e :token))]
    (insert! (assoc ?e :state ::renewed :failure (failure? r ?e)))))

(defrule download-certs
  "Download certs and trigger delivery for each host"
  [?e <- ::renewed [{:keys [failure]}] (= failure false)]
  =>
  (let [m1 (run ::mkdir ?e "/tmp/certs")]
    (doseq [[domain {:keys [id dest]}] (?e :domains)]
      (debug "downloading cert" domain)
      (let [m2 (run ::mkdir ?e (<< "/tmp/certs/~{domain}"))
            d1 (run ::download ?e domain "privkey.pem")
            d2 (run ::download ?e domain "cert.csr")
            failure (or (failure? d1 ?e) (failure? d2 ?e) (not m1) (not m2))]
        (insert! (assoc ?e :state ::downloaded :domain domain :ids [id] :dest dest :failure failure))))))

(defrule deliver-host-certs
  "Deliver a domain cert pair to a single host under a specified dest"
  [?e <- ::downloaded [{:keys [failure]}] (= failure false)]
  =>
  (let [{:keys [domain dest]} ?e
        r1 (run ::upload ?e dest domain "privkey.pem")
        r2 (run ::upload ?e dest domain "cert.csr")]
    (insert! (assoc ?e :state ::delivered :failure (or (failure? r1 ?e) (failure? r2 ?e))))))
