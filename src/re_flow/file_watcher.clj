(ns re-flow.file-watcher
  "Trigger facts based on file watch events"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-share.config.core :refer (get!)]
   [mount.core :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.session :refer (update-)]
   [juxt.dirwatch :refer (watch-dir close-watcher)]))

(refer-timbre)

(defn watch [{:keys [directory role] :as m}]
  (info (<< "file watching ~{directory} with role ~{role}"))
  (watch-dir (fn [e]
               (debug e)
               (update- [(merge e m {:state ::file})])) (clojure.java.io/file directory)))

(defstate watchers
  :start (doall (map watch (get! :shared :watch)))
  :stop  (doseq [w watchers]
           (debug "shutting down watcher")
           (close-watcher w)))

(comment
  (println ::file)
  (watch "/tmp/foo"))
