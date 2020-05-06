(ns re-flow.notification
  "Notification rules"
  (:require
   [clojure.core.strint :refer (<<)]
   [clara.rules :refer :all]
   [clojure.java.shell :refer (sh)]
   [re-mote.repl.publish :refer (send-email tofrom subject)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn notify [m]
  (sh "notify-send" "-t" "5000" m))

(defrule failure-osd-notify
  "Notify using OSD on all errors if running in a desktop machine"
  [?e <- :re-flow.core/state (= true (this :failure))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (notify (<< "Flow ~{flow} failed in ~{state} step"))))

(defrule failure-osd-notify
  "Notify using OSD on all errors if running in a desktop machine"
  [?e <- :re-flow.core/state (= true (this :failure))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (notify (<< "Flow ~{flow} succeeded"))))

(defrule failure-email
  "Notify using email if available"
  [?e <- :re-flow.core/state (= true (this :failure))]
  [:re-flow.session/type (= false (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (send-email (tofrom) (subject (<< "Flow ~{flow} failed in ~{state} step")))))

(defrule success-email-notify
  "Notify using email if available"
  [?e <- :re-flow.core/state (= false (this :failure))]
  [:re-flow.session/type (= false (this :desktop))]
  =>
  (let [{:keys [flow]} ?e]
    (send-email (tofrom) (subject (<< "Flow ~{flow} succeeded")))))
