(ns re-flow.nebula
  "Nebula cert deployment"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-flow.actions :refer (run)]
   [clara.rules.accumulators :as acc]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets materialize-preset)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (failure? into-ids)]
   [clara.rules :refer :all]))

(refer-timbre)
(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::uploaded :re-flow.core/state)
(derive ::sign :re-flow.core/state)
(derive ::signed :re-flow.core/state)
(derive ::delivered :re-flow.core/state)
(derive ::downloaded :re-flow.core/state)
(derive ::restarted :re-flow.core/state)
(derive ::done :re-flow.core/state)

(s/def ::sign-dest string?)

(s/def ::certs string?)

(s/def ::group string?)

(s/def ::hostname string?)

(s/def ::intermediary string?)

(s/def ::groups (s/coll-of ::group))

(s/def ::ip string?)

(s/def ::host (s/keys :req-un [::hostname ::groups ::ip]))

(s/def ::hosts (s/coll-of ::host))

(s/def ::nebula
  (s/keys :req-un [::certs ::sign-dest ::hosts ::intermediary]))

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
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance :provision? true)))

(defrule upload-cert
  "Upload sign cert and key"
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (debug "uploading cert and key to signing host")
  (let [r1 (run :upload ?e (<< "~(?e :certs)/ca.crt") (?e :sign-dest))
        r2 (run :upload ?e (<< "~(?e :certs)/ca.key") (?e :sign-dest))]
    (insert!
     (-> ?e
         (dissoc :message :failure)
         (assoc :state ::uploaded :failure (or (failure? ?e r1) (failure? ?e r2)))))))

(defrule signup
  "Sign host keys"
  [?e <- ::uploaded [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (debug "signing keys")
  (let [crt (<< "~(?e :sign-dest)/ca.crt")
        key (<< "~(?e :sign-dest)/ca.key")]
    (doseq [{:keys [hostname ip groups]} (?e :hosts)]
      (let [r (run ::sign ?e hostname ip groups crt key (?e :sign-dest))]
        (insert! (assoc ?e :state ::signed :failure (failure? ?e r) :hostname hostname))))))

(defrule download-nebula-keys
  "Download keys for host"
  [?e <- ::signed [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (debug "downloading certs for" (?e :hostname))
  (let [{:keys [intermediary hostname]} ?e
        m1 (run :mkdir ?e intermediary)
        d1 (run :download ?e (<< "~(?e :sign-dest)/~{hostname}.key") intermediary)
        d2 (run :download ?e (<< "~(?e :sign-dest)/~{hostname}.crt") intermediary)
        failure (or (failure? ?e d1) (failure? ?e d2) (not m1))]
    (insert! (assoc ?e :state ::downloaded :failure failure :ids (into-ids [hostname])))))

(defrule distribute
  "Fetch signed certs and distribute them to the hosts"
  [?e <- ::downloaded [{:keys [failure]}] (= failure false)]
  [?p <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  =>
  (info "distributing certs to host" (?e :hostname))
  (let [{:keys [hostname intermediary deploy-dest]} ?e
        r1 (run :upload ?e (<< "~{intermediary}/~{hostname}.key") (<< "~{deploy-dest}/~{hostname}.key"))
        r2 (run :upload ?e (<< "~{intermediary}/~{hostname}.crt") (<< "~{deploy-dest}/~{hostname}.crt"))
        r3 (run :upload ?e (<< "~(?e :certs)/ca.crt") (<< "~{deploy-dest}/ca.crt"))]
    (insert! (assoc ?e :state ::delivered :failure (or (failure? ?e r1) (failure? ?e r2) (failure? ?e r3))))))

(defrule restart
  "Restart nebula services on all signed hosts"
  [?e <- ::delivered [{:keys [failure]}] (= failure false)]
  [?s <- ::start]
  [?d <- (acc/count) :from [::delivered]]
  =>
  (when (= (count (?s :hosts)) ?d)
    (info "restarting service on" (?e :ids))
    (let [r (run :restart ?e "nebula")]
      (insert! (assoc ?e :state ::restarted :failure (failure? ?e r))))))

; Finalizing rules

(defrule success
  "Signup flow success"
  [?e <- ::restarted [{:keys [flow failure]}] (= failure false)]
  =>
  (let [{:keys [ids]} ?e]
    (insert! (assoc ?e :state ::done :message (<< "signup processes for ~{ids} was successful")))))

(defrule failure
  "Signup flow failure"
  [?e <- ::restarted [{:keys [flow failure]}] (= failure true)]
  =>
  (let [{:keys [ids]} ?e]
    (insert! (assoc ?e :state ::done :message (<< "signup processes for ~{ids} has failed")))))

(defrule cleanup
  "Cleanup sign instance when done distributing (we cleanup even on failure)"
  [?p <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::sign) (= failure false)]
  [?s <- ::start]
  [?d <- (acc/count) :from [::delivered]]
  =>
  (when (= (count (?s :hosts)) ?d)
    (debug "cleaning up sign instance")
    (insert! (assoc ?p :state :re-flow.setup/cleanup :re-flow.setup/purge true))))
