(ns re-flow.setup
  "Setting up a machine from creation to agent deployment and provisioning so its ready to be used"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.management :refer (registered?)]
   [re-share.wait :refer (wait-for)]
   [re-mote.repl :refer :all :exclude (provision)]
   [com.rpl.specter :refer [select ALL keypath]]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-core.repl :refer :all]
   [clara.rules :refer :all])
  (:import clojure.lang.ExceptionInfo))

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)
(refer-timbre)

(defn successful-ids [f]
  (select [ALL (keypath :results :success) ALL :args ALL :system-id] @f))

(derive ::creating ::state)
(derive ::created ::state)
(derive ::not-created ::state)
(derive ::registered ::state)
(derive ::not-registered ::state)
(derive ::provisioned ::state)
(derive ::not-provisioned ::state)

(defrule creating
  "Create systems"
  [?e <- ::creating []]
  =>
  (let [ids (successful-ids (create kvm defaults local c1-medium :backup "restore flow instance"))]
    (if-not (empty? ids)
      (insert! (assoc ?e :state ::created :ids ids))
      (insert! (assoc ?e :state ::not-created :failure true)))))

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
    (if (registraion-successful ids)
      (insert! (assoc ?e :state ::registered))
      (insert! (assoc ?e :state ::not-registered :failure true)))))

(defrule provisioning
  "Provisioning"
  [?e <- ::registered []]
  =>
  (let [{:keys [ids]} ?e]
    (info "provisioning" ids)
    (let [provisioned (successful-ids (provision (with-ids ids)))]
      (if (= provisioned ids)
        (insert! (assoc ?e :state ::provisioned))
        (insert! (assoc ?e :state ::not-provisioned :failure true))))))

(defquery get-failures
  "Find all failures"
  []
  [?f <- ::state (= true (this :failure))])

(defquery get-provisioned
  "Find all failures"
  []
  [?p <- ::provisioned])

(def session (atom (mk-session 're-flow.core :fact-type-fn :state)))

(defn setup! [f]
  (future
    (info "Starting the setup process" f)
    (reset! session (-> @session (insert {:state ::creating :flow f}) (fire-rules)))
    (info "Finished setup process" f)))

(comment
  (create! :dummy)
  (query @session get-provisioned)
  (query @session get-failures))
