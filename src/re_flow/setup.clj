(ns re-flow.setup
  "Setting up a machine from creation to agent deployment and provisioning so its ready to be used"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.management :refer (registered?)]
   [re-share.wait :refer (wait-for)]
   [re-mote.repl :refer :all :exclude (provision)]
   [re-core.repl :refer :all]
   [re-flow.common :refer (successful-systems)]
   [clara.rules :refer :all])
  (:import clojure.lang.ExceptionInfo))

(refer-timbre)

(defn create-instance [base args]
  (apply create base args))

(derive ::creating :re-flow.core/state)
(derive ::created :re-flow.core/state)
(derive ::registered :re-flow.core/state)
(derive ::provisioned :re-flow.core/state)

(defrule creating
  "Create systems"
  [?e <- ::creating []]
  =>
  (let [{:keys [base args]} (:spec ?e)
        ids (successful-systems (create-instance base args))]
    (insert! (assoc ?e :state ::created :ids ids :failure (empty? ids)))))

(defn registraion-successful [ids]
  (let [hs (hosts (with-ids ids) :hostname)]
    (try
      (wait-for {:timeout [1 :minute]}
                (fn [] (empty? (filter (comp not registered?) (:hosts hs))))
                "Failed to wait for agents to register")
      true
      (catch ExceptionInfo _ false))))

(defrule registing
  "Registering"
  [?e <- ::created []]
  =>
  (let [gent "/home/ronen/code/re-ops/re-gent/target/re-gent"
        {:keys [ids]} ?e]
    (info "deploying agent to" ids)
    (deploy (hosts (with-ids ids) :ip) gent)
    (insert! (assoc ?e :state ::registered :failure (not (registraion-successful ids))))))

(defn run-provisioning [ids]
  (provision (with-ids ids)))

(defrule provisioning
  "Provisioning"
  [?e <- ::registered []]
  =>
  (let [{:keys [ids]} ?e]
    (info "provisioning" ids)
    (let [provisioned (successful-systems (run-provisioning ids))]
      (insert! (assoc ?e :state ::provisioned :failure (not= provisioned ids))))))
