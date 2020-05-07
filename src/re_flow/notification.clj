(ns re-flow.notification
  "Notification rules"
  (:require
   [clojure.core.strint :refer (<<)]
   [clara.rules :refer :all]
   [clojure.java.shell :refer (sh)]
   [re-mote.publish.email :refer (send-email tofrom)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn notify [m]
  (sh "notify-send" "-t" "5000" m))

(defrule failure-osd
  "Notify using OSD on all errors if running in a desktop machine"
  [?e <- :re-flow.core/state (= true (this :failure))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (notify (<< "Flow ~{flow} failed in ~{state} step"))))

(defrule success-osd
  "Notify OSD if message is present"
  [?e <- :re-flow.core/state (= false (this :failure)) (not (nil? (this :message)))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow message]} ?e]
    (notify message)))

(defrule failure-email
  "Notify using email if available"
  [?e <- :re-flow.core/state (= true (this :failure))]
  [:re-flow.session/type (= false (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (send-email (<< "Flow ~{flow} result") (tofrom) (<< "Flow ~{flow} failed in ~{state} step"))))

(defrule success-email-notify
  "Email if message is present"
  [?e <- :re-flow.core/state (= false (this :failure)) (not (nil? (this :message)))]
  [:re-flow.session/type (= false (this :desktop))]
  =>
  (let [{:keys [flow message]} ?e]
    (send-email (<< "Flow ~{flow} result") (tofrom) message)))
