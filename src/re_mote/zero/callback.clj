(ns re-mote.zero.callback
  "Callback handling for run-hosts async"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.schedule :refer [watch seconds]]
   [re-share.wait :refer [wait-time curr-time]]
   [re-mote.zero.results :refer (collect)]))

(refer-timbre)

(def callbacks (atom {}))

(defn check-results []
  (doseq [[uuid {:keys [f timeout stamp hosts]}] @callbacks]
    (if-let [results (collect (keys hosts) uuid)]
      (do
        (future (f false results))
        (swap! callbacks dissoc uuid))
      (when-not (> (wait-time stamp timeout) (curr-time))
        (error "failed to get callback results within provided timeout range for uuid" uuid)
        (future (f true {}))
        (swap! callbacks dissoc uuid)))))

(defn callback-watch
  "Initalize callback processing job"
  []
  (watch :callback-processing (seconds 1) check-results))

(defn register-callback [hosts uuid timeout f]
  (swap! callbacks assoc uuid {:f f :timeout timeout :stamp (curr-time) :hosts hosts}))
