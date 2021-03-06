(ns re-mote.repl.publish
  (:require
   ; publishing
   [re-mote.publish.email :refer (send-email tofrom)]
   [re-mote.publish.riemann :refer (send-event into-events)]
   [re-share.config.core :as conf]
   [clojure.java.io :refer (file)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-share.core :refer (gen-uuid)]
   [re-mote.log :refer (get-logs)]
   [re-mote.repl.base :refer (refer-base)]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [html5 include-js include-css]])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Publishing
  (email [this m desc])
  (riemann [this m]))

(defn save-fails [{:keys [failure]}]
  (let [stdout (<< "/tmp/~(gen-uuid).out.txt") stderr (<< "/tmp/~(gen-uuid).err.txt")]
    (doseq [[c rs] failure]
      (doseq [{:keys [host error out]} (get-logs rs)]
        (do (spit stdout (str host ": " out "\n") :append true)
            (spit stderr (str host ": " error "\n") :append true))))
    [stdout stderr]))

(defn summarize [^String s]
  (let [l (.length s)]
    (if (< l 50) s (.substring s (- l 50) l))))

(defn template
  "Success and failure template"
  [{:keys [success failure]}]
  (html
   (html5
    [:head]
    [:body
     [:h3 "Success:"]
     [:ul
      (for [{:keys [host out]} success] [:li " &#10003;" host])]
     [:h3 "Failure:"]
     [:ul
      (for [[c rs] failure]
        (for [{:keys [host error out]} (get-logs rs)]
          [:li " &#x2717;" " " host " - " (if out (str c ",") "") (or error (summarize out))]))]
     [:p "For more information please check you local log provider."]])))

(defn attachments [m]
  (let [attachment (fn [f] {:type :attachment :content (file f)})]
    (map attachment (filter (fn [f] (.exists (file f))) (save-fails m)))))

(extend-type Hosts
  Publishing
  (email [this m desc]
    (send-email (<< "Running ~{desc} results") (tofrom) (template m) (attachments m))
    [this m])

  (riemann [this {:keys [success failure] :as m}]
    (doseq [v success]
      (doseq [e (into-events v)]
        (send-event (assoc e :tags ["success"]))))
    (doseq [[code es] failure]
      (doseq [e es]
        (send-event (merge e {:tags ["failure"] :code code}))))
    [this m]))

(defn refer-publish []
  (require '[re-mote.repl.publish :as pub :refer (email riemann)]))
