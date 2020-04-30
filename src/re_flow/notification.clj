(ns re-flow.notification
  "Notification rules"
  (:require
   [clojure.core.strint :refer (<<)]
   [clara.rules :refer :all]
   [clojure.java.shell :refer (sh)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn notify [m]
  (sh "notify-send" "-t" "5000" m))

(defrule osd-notify
  "Notify using OSD on all errors if running in a desktop machine"
  [?e <- :re-flow.core/state (= true (this :failure)) (not (nil? (this :message)))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (notify (<< "Flow ~{flow} failed in ~{state} step"))))

(defrule email-notify
  "Notify using email if available"
  [?e <- :re-flow.core/state (= true (this :failure)) (not (nil? (this :message)))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (notify (<< "Flow ~{flow} failed in ~{state} step"))))
