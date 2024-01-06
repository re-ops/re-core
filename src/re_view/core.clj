(ns re-view.core
  "Viewing and reacting to the state of the world"
  (:require
   [re-flow.pubsub :refer [publish-fact]]
   [taoensso.timbre :refer (refer-timbre)]
   [re-share.schedule :refer (watch seconds)]
   [re-view.xtdb :as xtdb :refer [query submit entity]]))

(refer-timbre)

(defn pending-actions []
  (query '{:find [(pull e [*])] :where [[e :re-view.task/action _]
                                        [e :re-view.task/target :re-core]
                                        [e :re-view.task/status :pending]]}))
(defn running-actions []
  (query '{:find [(pull e [*])] :where [[e :re-view.task/action _]
                                        [e :re-view.task/target :re-core]
                                        [e :re-view.task/status :running]]}))

(defn mark-running [id]
  (let [e (entity id)]
    (submit [[:xtbd.api/put (assoc e :re-view.task/status :running)]])))

(defn watch-events []
  (watch :pending-actions (seconds 10)
         (fn []
           (let [ps (pending-actions)]
             (doseq [[p] ps]
               (info (assoc p :state :re-flow.view/start))
               (publish-fact (assoc p :state :re-flow.view/start)))))))

(comment
  (pending-actions)
  (running-actions)
  (mark-running 12)
  (entity 12)
  ; insert a fact on a pending action
  (submit [[:xtdb.api/put
            {:xt/id 13
             :re-view.task/source :re-bot
             :re-view.task/target :re-core
             :re-view.task/action :stop
             :re-view.task/args {:id "d8ee"}
             :re-view.task/status :pending}]]))
