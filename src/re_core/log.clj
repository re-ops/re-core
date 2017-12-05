(ns re-core.log
  (:require
   [re-core.common :refer (get! get* version)]
   [gelfino.timbre :refer (set-tid get-tid gelf-appender)]
   [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
   [taoensso.timbre.appenders.core :refer (println-appender)]
   [taoensso.timbre  :as timbre :refer (merge-config! set-level! refer-timbre)]))

(refer-timbre)

(defn disable-coloring
  "See https://github.com/ptaoussanis/timbre"
  []
  (merge-config! {:output-fn (partial timbre/default-output-fn  {:stacktrace-fonts {}})}))

(defn setup-logging
  "Sets up logging configuration"
  []
  (let [log* (partial get* :re-core :log)]
    (when (log* :gelf)
      (merge-config!
       {:appenders {:gelf (gelf-appender {:host (log* :gelf :host)})}}))
    (merge-config!
     {:appenders {:rolling (rolling-appender {:path (log* :path) :pattern :weekly})}})
    (merge-config!
     {:ns-blacklist ["net.schmizz.*" "org.elasticsearch.indices.*"]})
    (merge-config!
     {:appenders {:println {:ns-whitelist ["re-core"]}}})
    (disable-coloring)
    (set-level! (or (log* :level) :debug))))

(defn debug-on []
  (set-level! :debug))

(defn info-on []
  (set-level! :info))

(defn trace-on []
  (set-level! :trace))

(defn sshj-debug
  []
  (merge-config! {:ns-blacklist ["org.elasticsearch.indices.*"]})
  (debug-on))
