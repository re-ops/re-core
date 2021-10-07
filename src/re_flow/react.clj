(ns re-flow.react
  "Reacting to agent status changes per host"
  (:require
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(derive ::request :re-flow.core/state)
(derive ::down :re-flow.core/state)

(defrule request
  "Processing incoming requests (registration, un-registration)"
  [?e <- ::request]
  =>
  (info "Reacing to" ?e))

(defrule instance-down
  "Instance went down"
  [?e <- ::down]
  =>
  (info "Reacting to instance down" ?e))
