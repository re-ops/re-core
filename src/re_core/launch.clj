(ns re-core.launch
  "re-core lanching ground aka main"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-core.log :refer (setup-logging)]
   [re-core.common :refer (version)]
   [re-share.config :as conf]
   [re-core.queue :refer (queue)]
   [re-core.workers :refer (workers)]
   [re-core.schedule :refer (schedule)]
   [re-share.components.elastic :as es]
   [mount.core :as mount]
   [es.common :refer (types index)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn add-shutdown []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn [] (mount/stop)))))

(defn setup
  "One time setup"
  []
  (setup-logging)
  (conf/load (fn [_] {}))
  (add-shutdown))

(defn start
  "Main components startup (jetty, job workers etc..)"
  []
  (conf/load (fn [_] {}))
  (mount/start #'schedule #'queue #'workers)
  (info (<<  "version ~{version} see https://github.com/re-ops/re-core")))

(defn stop
  "stopping the application"
  []
  (mount/stop))

(defn -main [& args]
  (start))

(comment
  (setup)
  (start)
  (stop))
