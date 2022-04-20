(ns re-flow.notification
  "Notification rules"
  (:require
   [re-flow.pubsub :refer (publish-?e publish-fact)]
   [clojure.core.strint :refer (<<)]
   [clara.rules :refer :all]
   [clojure.java.shell :refer (sh)]
   [re-mote.publish.email :refer (send-email tofrom)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(derive ::notify :re-flow.core/state)

(defn notify [m]
  (sh "notify-send" "-t" "5000" m))

(defrule default-failure-message
  "Format failure notification if no message/subject were provided"
  [?e <- :re-flow.core/state (= true (this :failure)) (nil? (this :message)) (nil? (this :subject))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [flow state]} ?e]
    (insert! (assoc ?e :state ::notify :message (<< "Flow ~{flow} failed in ~{state} step") :subject (<< "Flow ~{flow} has failed")))))

(defrule osd-notify
  "Notify using OSD on desktop systems"
  [?e <- :re-flow.core/state (not (nil? (this :message)))]
  [:re-flow.session/type (= true (this :desktop))]
  =>
  (let [{:keys [message]} ?e]
    (notify message)))

(defrule email-notify
  "A catch all failure notification email if no message is present"
  [?e <- :re-flow.core/state (not (nil? (this :message))) (not (nil? (this :subject)))]
  [:re-flow.session/type (= false (this :desktop)) (= true (this :smtp))]
  =>
  (let [{:keys [subject message]} ?e]
    (send-email subject (tofrom) message)))

(defrule log-fallback
  "Log fallback if headless and smtp isn't configured"
  [?e <- :re-flow.core/state (not (nil? (this :message)))]
  [:re-flow.session/type (= false (this :desktop)) (= false (this :smtp))]
  =>
  (let [{:keys [message]} ?e]
    (info message)))

#_(defrule notify-promise
    "Triggering notification rules using core.async channels for any ?e containing a message"
    [?e <- :re-flow.core/state]
    =>
    (debug "publishing" (?e :message))
    (publish-?e ?e))

(comment
  (publish-fact {:state ::notify :subject "Running  results" :message "hello" :failure false}))
