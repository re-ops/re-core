(ns re-core.log
  (:require
   [re-share.log :as log]
   [re-core.common :refer (get! get* version)]
   [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
   [taoensso.timbre.appenders.core :refer (println-appender)]
   [taoensso.timbre  :as timbre :refer (merge-config! set-level! refer-timbre)]))

(refer-timbre)

(defn disable-coloring
  "See https://github.com/ptaoussanis/timbre"
  []
  (merge-config! {:output-fn (partial timbre/default-output-fn  {:stacktrace-fonts {}})}))

#_(defn setup-logging
    "Sets up logging configuration"
    []
    (let [log* (partial get* :re-core :log)]
      (merge-config!
       {:appenders {:rolling (rolling-appender {:path (log* :path) :pattern :weekly})}})
      (merge-config!
       {:ns-blacklist ["net.schmizz.*" "org.elasticsearch.*" "org.apache.http.*"]})
      (merge-config!
       {:appenders {:println {:ns-whitelist ["re-core" "re-mote" "re-share"]}}})
      (disable-coloring)
      (set-level! (or (log* :level) :info))))

(defn setup-logging
  "Sets up logging configuration:
    - stale logs removale interval
    - steam collect logs
    - log level
  "
  [& {:keys [interval level] :or {interval 10 level :info}}]
  (log/setup "re-core" ["net.schmizz.*" "org.elasticsearch.*" "org.apache.http.*"] ["re-mote.output"])
  (set-level! level))

