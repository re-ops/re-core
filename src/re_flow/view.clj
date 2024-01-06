(ns re-flow.view
  "Re-view processing flow:
    * Trigger a flow based on pending actions found
    * Track the execution of the flow
    * Update the view back with the flow restults
  "
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.spec :refer (valid?)]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [clojure.edn :as edn]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   #_[re-view.core :refer (mark-running)]
   [re-flow.common :refer (run-?e run-?e-non-block results successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::run :re-flow.core/state)
(derive ::timedout :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(def a-system? #{:re-bot :re-core})

(s/def :re-view.task/source (s/and keyword? a-system?))

(s/def :re-view.task/target (s/and keyword? a-system?))

(s/def :re-view.task/status (s/and keyword? #{:pending :running :done :failed :timedout}))

(s/def :re-view.task/action keyword?)

(s/def :re-view.task/args map?)

(s/def ::action
  (s/keys :req [:re-view.task/source :re-view.task/target :re-view.task/action :re-view.task/args :re-view.task/status]))

(defrule check
  "Check that the fact is matching the ::restore spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::action ?e))]
    (info (expound/expound-str ::action ?e))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate view action spec")))))

(defrule trigger
  "Triggering pending actions"
  [?e <- ::spec [{:keys [failure]}] (= failure false) (= (get ?e :re-view.task/status) :pending)]
  =>
  (info "Triggering action" ?e)
  #_(mark-running (:xt/id ?e))
  #_(insert! (assoc ?e :state (keyword "re-view.registry" action) :re-view.task/status :running)))
