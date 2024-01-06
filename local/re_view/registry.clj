(ns re-view.registry
  (:require
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(derive ::change-state :re-flow.core/state)

; Re-core basic actions
(defrule change-instance-state
  "Start an instance"
  [?e <- ::change-state [{:keys [state]}] (= state :running)]
  =>
  (let [{:keys [id state]} (?e :args)]
    (info (<< "Changing instance ~{id} state into ~{state}"))))

