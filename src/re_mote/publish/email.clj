(ns re-mote.publish.email
  "Generating run result html email"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-share.config.core :as conf]
   [postal.core :as p :refer (send-message)]
   [re-mote.log :refer (get-logs)]))

(defn tofrom
  "Email configuration used to send emails"
  []
  (merge (conf/get! :shared :email)))

(defn send-html-email
  "Send HTML format email"
  [subject address msg]
  (let [body {:body [{:type "text/html" :content msg}]}]
    (send-message (conf/get! :shared :smtp) (merge address {:subject subject} body))))

(comment
  (send-html-email "foo" (tofrom)))
