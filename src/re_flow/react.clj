(ns re-flow.react
  "Reacting to agent status changes per host"
  (:require
   [re-share.core :refer (gen-uuid)]
   [es.systems :as sys]
   [re-flow.common :refer (into-ids)]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(derive ::request :re-flow.core/state)
(derive ::typed :re-flow.core/state)
(derive ::down :re-flow.core/state)
(derive ::cleanup :re-flow.core/state)

(defn enrich [?e id]
  (assoc ?e :system (sys/get id)))

(defrule register
  "Processing registration requests"
  [?e <- ::request [{:keys [request]}] (= request "register")]
  =>
  (let [uuid (gen-uuid)]
    (debug "Reacting to instance registration")
    (let [id (first (into-ids [(?e :hostname)]))]
      (insert! (assoc (enrich ?e id) :state ::typed :ids [id] :uuid uuid)))))

(defrule unregister
  "Processing incoming un-registration requests"
  [?e <- ::request [{:keys [request]}] (= request "unregister")]
  =>
  (debug "Reacting to instance un-registration" ?e)
  (let [id (first (into-ids [(?e :hostname)]))]
    (insert! (assoc (enrich ?e id) :state ::cleanup :ids [id]))))

(defrule instance-down
  "Instance went down"
  [?e <- ::down]
  =>
  (debug "Reacting to instance going down" ?e)
  (let [id (first (into-ids [(?e :hostname)]))]
    (insert! (assoc (enrich ?e id) :state ::cleanup :ids [id]))))
