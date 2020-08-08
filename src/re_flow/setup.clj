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
(derive ::cleanup :re-flow.core/state)
(derive ::purged :re-flow.core/state)
(derive ::registered :re-flow.core/state)
(derive ::provisioned :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defrule creating
  "Create systems"
  [?e <- ::creating []]
  =>
  (let [{:keys [base args]} (:spec ?e)
        result (create-instance base args)
        ids (successful-systems result)]
    (insert! (assoc ?e :state ::created :result @result :ids ids :failure (empty? ids)))))

(defrule creation-failed
  "Creation failure"
  [?e <- ::created [{:keys [failure result]}] (= failure true)]
  =>
  (info "creation failed due to:\n" (clojure.string/join "\n" (map :message (-> ?e :result second :results :failure)))))

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
  [?e <- ::created [{:keys [failure]}] (= failure false)]
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
  [?e <- ::registered [{:keys [failure]}] (= failure false)]
  =>
  (let [{:keys [ids]} ?e]
    (info "provisioning" ids)
    (let [provisioned (successful-systems (run-provisioning ids))]
      (insert! (assoc ?e :state ::provisioned :failure (not= provisioned ids) :message "instance provisioned successfully")))))

(defn purge-instances [ids]
  (destroy (with-ids ids) {:force true}))

(defrule cleanup
  "Cleanup instance"
  [?e <- ::cleanup [{:keys [::purge]}] (= purge true)]
  =>
  (let [{:keys [ids]} ?e
        purged (successful-systems (purge-instances ids))]
    (info "purged instances" purged)
    (insert! (assoc ?e :state ::purged :purged purged :failure (not= purged ids)))))
