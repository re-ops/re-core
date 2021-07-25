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

(s/def ::intermediary string?)

(s/def ::dest string?)

(s/def ::id (s/and string? #(= (.length %) 20)))

(s/def ::delivery (s/keys :req-un [::id ::dest]))

(s/def ::domains (s/map-of ::domain ::delivery))

(s/def ::certs
  (s/keys :req-un [::domains ::user ::token ::intermediary]))

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
         (assoc :state ::domains-ready :failure (failure? ?e r))))))

(defrule run-renewal
  "Renew the certificates"
  [?e <- ::domains-ready [{:keys [failure]}] (= failure false)]
  =>
  (let [r (run ::renew ?e (?e :user) (?e :token))]
    (insert! (assoc ?e :state ::renewed :failure (failure? ?e r)))))

(defrule download-certs
  "Download certs and trigger delivery for each host"
  [?e <- ::renewed [{:keys [failure]}] (= failure false)]
  =>
  (let [{:keys [intermediary domains]} ?e
        m1 (run :mkdir ?e intermediary)]
    (doseq [[domain {:keys [id dest]}] domains
            :let [inter-target (<< "~{intermediary}/~{domain}")]]
      (debug "downloading cert" domain)
      (let [src (<< "/srv/dehydrated/certs/~{domain}")
            m2 (run :mkdir ?e inter-target)
            d1 (run :download ?e (<< "~{src}/privkey.pem") inter-target)
            d2 (run :download ?e (<< "~{src}/fullchain.pem") inter-target)
            d3 (run :download ?e (<< "~{src}/cert.pem") inter-target)
            failure (or (failure? ?e d1) (failure? ?e d2) (failure? ?e d3) (not m1) (not m2))]
        (insert! (assoc ?e :state ::downloaded :domain domain :ids [id] :dest dest :failure failure))))))

(defrule deliver-host-certs
  "Deliver a domain cert pair to a single host under a specified dest (if they are provided)"
  [?e <- ::downloaded [{:keys [failure dest ids]}] (= failure false) (not (nil? dest)) (not (empty? (filter identity ids)))]
  =>
  (let [{:keys [domain dest intermediary]} ?e
        r1 (run :upload ?e dest (<< "~{intermediary}/~{domain}/privkey.pem"))
        r2 (run :upload ?e dest (<< "~{intermediary}/~{domain}/fullchain.pem"))
        r3 (run :upload ?e dest (<< "~{intermediary}/~{domain}/cert.pem"))]
    (insert! (assoc ?e :state ::delivered :failure (or (failure? ?e r1) (failure? ?e r2) (failure? ?e r3))))))
