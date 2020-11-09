(ns re-flow.certs
  "Lets encrypt cert renewal flow"
  (:require
   [me.raynes.fs :refer (mkdir)]
   [re-mote.zero.certs :refer (refer-certs)]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (scp-from scp-into)]
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
(derive ::copied :re-flow.core/state)
(derive ::distribute :re-flow.core/state)
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
  {:base kvm :args [defaults local small :letsencrypt (<< "letsencrypt cert generation and distribution")]})

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
  (let [r (run-?e set-domains ?e (?e :domains))]
    (insert!
     (-> ?e
         (dissoc :message :failure)
         (assoc  :state ::domains-ready :failure (failure? r ?e))))))

(defrule run-renewal
  "Renew the certificates"
  [?e <- ::domains-ready [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (let [r (run-?e renew ?e (?e :user) (?e :token))]
    (insert! (assoc ?e :state ::renewed :failure (failure? r ?e)))))

(defrule copy-certs
  "Grab certs back"
  [?e <- ::renewed [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (info "renewed certs was successful")
  (mkdir "/tmp/certs")
  (doseq [domain (?e :domains)]
    (info "copying" (<< "/srv/dehydrated/certs/~{domain}/privkey.pem"))
    (let [r1 (run-?e scp-from (assoc ?e :pick-by :ip) (<< "/srv/dehydrated/certs/~{domain}/privkey.pem") "/tmp/certs/")
          r2 (run-?e scp-from (assoc ?e :pick-by :ip) (<< "/srv/dehydrated/certs/~{domain}/cert.csr") "/tmp/certs/")]
      (insert! (assoc ?e :state ::copied :copied-domain domain :failure (or (failure? r1 ?e) (failure? r2 ?e)))))))

(defrule deliver-
  "Deliver cert to host"
  [?e <- ::copied [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (info "copied cert" (?e :copied-domain)))
