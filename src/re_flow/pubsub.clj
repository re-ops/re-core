(ns re-flow.pubsub
  "In memory pubsub channels for rules firing"
  (:require
   [re-share.core :refer (gen-uuid)]
   [re-core.queue :refer [enqueue]]
   [taoensso.timbre :refer (info)]
   [mount.core :as mount :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.core.async :refer (chan dropping-buffer pub >!! <!! sub close!)]))

(refer-timbre)

(defn- initialize []
  (let [input (chan (dropping-buffer 100))]
    {:input input
     :pub (pub input :state)}))

(defstate ^{:on-reload :noop} rules-pubsub
  :start (initialize)
  :stop (close! (rules-pubsub :input)))

(defn publish-fact [?e]
  (enqueue :re-flow.session/facts {:tid (gen-uuid) :args [[?e]]}))

(defn publish-?e
  "Publish an ?e"
  [?e]
  {:pre [(isa? (?e :state) :re-flow.core/state)]}
  (>!! (rules-pubsub :input) ?e))

(defn subscribe-?e
  "Subsribe to ?e with state"
  [state c]
  (sub (rules-pubsub :pub) state c) c)

(comment
  (publish-?e {:state :re-flow.certs/copied})
  (future
    (let [output (subscribe-?e :re-flow.certs/copied (chan))]
      (loop []
        (Thread/sleep 1000)
        (info (<!! output))
        (recur)))))
