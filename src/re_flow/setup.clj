(ns re-flow.setup
  "Setting up a machine from creation to agent deployment and provisioning so its ready to be used"
  (:require
   [re-share.config.core :refer (get!)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.management :refer (registered?)]
   [re-share.wait :refer (wait-for)]
   [re-mote.repl :refer :all :exclude (provision)]
   [re-core.repl :refer :all]
   [re-core.networking :refer (ips-available)]
   [re-flow.common :refer (successful-systems)]
   [clojure.core.strint :refer (<<)]
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
(derive ::available :re-flow.core/state)
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

(defrule available
  "The ip addresses of all instances is available and ready (this can be delayed by storage flush delay)"
  [?e <- ::created [{:keys [failure]}] (= failure false)]
  =>
  (let [{:keys [ids]} ?e]
    (insert! (assoc ?e :state ::available :failure (not (ips-available ids))))))

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
  [?a <- ::available [{:keys [failure ids]}] (= failure false) (= (hash-set ids) (hash-set (?e :ids)))]
  =>
  (let [gent (get! :re-gent :bin)
        {:keys [ids]} ?e]
    (debug "deploying agent to" ids)
    (deploy (hosts (with-ids ids) :ip) gent)
    (insert! (assoc ?e :state ::registered :failure (not (registraion-successful ids))))))

(defn run-provisioning [ids]
  (provision (with-ids ids)))

(defn update-repo [ids]
  (update (hosts (with-ids ids) :hostname)))

(defrule provisioning
  "Provisioning"
  [?e <- ::registered [{:keys [failure provision?]}] (= failure false) (= provision? true)]
  =>
  (let [{:keys [ids]} ?e]
    (debug "updating host repository" ids)
    (update-repo ids)
    (debug "provisioning" ids)
    (let [provisioned (successful-systems (run-provisioning ids))]
      (doseq [id provisioned]
        (insert! (assoc ?e :state ::provisioned :failure false :id id :message (<< "instance ~{id} provisioned successfully"))))
      (doseq [id (filter (comp not (into #{} provisioned)) ids)]
        (insert! (assoc ?e :state ::provisioned :failure true :id id :message (<< "instance ~{id} failed to provision")))))))

(defn purge-instances [ids]
  (destroy (with-ids ids) {:force true}))

(defrule cleanup
  "Cleanup instance"
  [?e <- ::cleanup [{:keys [::purge]}] (= purge true)]
  =>
  (let [{:keys [ids]} ?e
        purged (successful-systems (purge-instances ids))]
    (insert! (assoc ?e :state ::purged :purged purged :failure (not= purged ids)))))
