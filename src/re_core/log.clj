(ns re-core.log
  (:require
   [re-share.log :as log]
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
  [& {:keys [level] :or {level :info}}]
  (let [blacklist ["net.schmizz" "org.elasticsearch" "org.apache.http" "com.hierynomus.sshj.userauth.keyprovider" "xtdb.system" "xtdb.query" "xtdb.tx"]]
    (log/setup "re-core" blacklist))
  (set-level! level))

