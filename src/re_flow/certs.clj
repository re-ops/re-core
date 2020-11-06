(ns re-flow.certs
  "Lets encrypt cert renewal flow"
  (:require
   [re-mote.zero.certs :refer [setup-domains cert-renew]]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (scp-from scp-into)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets materialize-preset)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e run-?e-non-block results successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::domains-ready :re-flow.core/state)
(derive ::renewed :re-flow.core/state)
(derive ::distribute :re-flow.core/state)
(derive ::timedout :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(s/def ::timeout integer?)

(s/def ::user string?)

(s/def ::token string?)

(s/def ::domains (s/coll-of :re-core.specs/domain))

(s/def ::destination string?)

(s/def ::delivery (s/keys :req [::destination :re-core.specs/domain]))

(s/def ::distribution (s/map-of :re-mote.spec/host ::delivery))

(s/def ::certs
  (s/keys :req [::domains ::user ::token ::timeout ::distribution]))

(def instance
  {:base kvm :args [defaults local small :letsencrypt (<< "letsencrypt cert generation and distribution")]})

(defrule check
  "Check that the fact is matching the ::certs spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::certs ?e))]
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? (expound/expound-str ::certs ?e))))))

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
  (let [r (run-?e setup-domains ?e (?e :domains))]
    (insert! (assoc ?e :state ::domains-ready :failure (= (successful-ids r) (?e :ids))))))

(defrule renew
  "Renew the certificates"
  [?e <- ::domains-ready [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (let [r (run-?e cert-renew ?e [(?e :user) (?e :token)])]
    (insert! (assoc ?e :state ::renewed :failure (= (successful-ids r) (?e :ids))))))

(defrule distribute
  "Distribute certificates to remote hosts"
  [?e <- ::renewed [{:keys [flow failure]}] (= flow ::certs) (= failure false)]
  =>
  (let [r (run-?e cert-renew ?e [(?e :user) (?e :token)])]
    (insert! (assoc ?e :state ::distributed :failure (= (successful-ids r) (?e :ids))))))
