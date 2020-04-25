(ns re-flow.notification
  "Notification rules"
  (:require
   [re-cog.facts.datalog :refer (desktop?)]
   [clojure.core.strint :refer (<<)]
   [clara.rules :refer :all]
   [clojure.java.shell :refer (sh)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn notify [m]
  (when (desktop?)
    (sh "notify-send" "-t" "5000" m)))

(defrule osd-notify-failures
  "Notify using OSD on all errors if running in a desktop machine"
  [?e <- :re-flow.core/state (= true (this :failure)) (not (nil? (this :message)))]
  =>
  (notify (<< "Flow ~(:flow ?e) failed in ~(:state ?e) step"))
  (info (:message ?e)))
