(ns re-mote.repl.publish
  (:require
   ; publishing
   [postal.core :as p :refer (send-message)]
   [re-mote.publish.email :refer (template)]
   [re-mote.publish.riemann :refer (send-event into-events)]
   [re-share.config.core :as conf]
   [clojure.java.io :refer (file)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-share.core :refer (gen-uuid)]
   [re-mote.log :refer (get-logs)]
   [re-mote.repl.base :refer (refer-base)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Publishing
  (email [this m address sub])
  (riemann [this m]))

(defn save-fails [{:keys [failure]}]
  (let [stdout (<< "/tmp/~(gen-uuid).out.txt") stderr (<< "/tmp/~(gen-uuid).err.txt")]
    (doseq [[c rs] failure]
      (doseq [{:keys [host error out]} (get-logs rs)]
        (do (spit stdout (str host ": " out "\n") :append true)
            (spit stderr (str host ": " error "\n") :append true))))
    [stdout stderr]))

(defn send-email [m address sub]
  (let [body {:type "text/html" :content (template m)}
        attachment (fn [f] {:type :attachment :content (file f)})
        files (map attachment (filter (fn [f] (.exists (file f))) (save-fails m)))
        message (merge address sub {:body (into [:alternative body] files)})]
    (send-message (conf/get! :re-mote :smtp) message)))

(extend-type Hosts
  Publishing
  (email [this m address sub]
    (send-email m address sub)
    [this m])

  (riemann [this {:keys [success failure] :as m}]
    (doseq [v success]
      (doseq [e (into-events v)]
        (send-event (assoc e :tags ["success"]))))
    (doseq [[code es] failure]
      (doseq [e es]
        (send-event (merge e {:tags ["failure"] :code code}))))
    [this m]))

(defn tofrom
  "Email configuration used to send emails"
  []
  (merge (conf/get! :shared :email)))

(defn subject [m desc]
  (assoc :m {:subject (<< "Running ~{desc} results")}))

(defn refer-publish []
  (require '[re-mote.repl.publish :as pub :refer (email riemann tofrom subject)]))
