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

(defn send-email
  ([subject address body]
   (send-email body address subject nil))
  ([subject address body attachments]
   (let [body' (if-not attachments {:body body} {:body (into [:alternative body] attachments)})
         message (merge address {:subject subject} body')]
     (send-message (conf/get! :shared :smtp) message))))

(comment
  (send-email "foo" (tofrom) "hello"))
