(ns re-core.log
  (:require
   [re-share.log :as log]
   [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
   [taoensso.timbre.appenders.core :refer (println-appender)]
   [taoensso.timbre  :as timbre :refer (merge-config! set-level! refer-timbre)]))

(refer-timbre)

(defn disable-coloring
  "See https://github.com/ptaoussanis/timbre"
  []
  (merge-config! {:output-fn (partial timbre/default-output-fn  {:stacktrace-fonts {}})}))

(defn setup-logging
  "Sets up logging configuration:
    - stale logs removale interval
    - steam collect logs
    - log level
  "
  [& {:keys [interval level] :or {interval 10 level :info}}]
  (log/setup "re-core" ["net.schmizz.*" "org.elasticsearch.*" "org.apache.http.*"] ["re-mote.output"])
  (set-level! level))

