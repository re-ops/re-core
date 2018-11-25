(ns re-core.launch
  "re-core lanching ground aka main"
  (:require
   [re-core.log :refer (setup-logging)]
   [re-core.common :refer (version)]
   [re-share.config :as conf]
   [re-core.queue :as q]
   [re-core.workers :as w]
   [re-core.schedule :as sch]
   [clojure.core.strint :refer (<<)]
   [clojure.java.io :refer (resource)]
   [re-share.components.core :refer (start-all stop-all setup-all)]
   [re-share.components.elastic :as es]
   [es.common :refer (types index)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn build-components [] {:es (es/instance types :re-core false) :queues (q/instance)
                           :schedule (sch/instance) :workers (w/instance)})

(defn clean-up
  "Clean/release resources, used also as a shutdown hook"
  [components]
  (debug "Shutting down...")
  (stop-all components))

(defn add-shutdown [components]
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (clean-up components)))))

(defn setup
  "One time setup"
  []
  (let [components (build-components)]
    (setup-logging)
    (conf/load (fn [_] {}))
    (add-shutdown components)
    (setup-all components)
    components))

(defn start
  "Main components startup (jetty, job workers etc..)"
  [components]
  (conf/load (fn [_] {}))
  (start-all components)
  (info (<<  "version ~{version} see https://github.com/re-ops/re-core"))
  components)

(defn stop
  "stopping the application"
  [components]
  (clean-up components)
  components)

(defn -main [& args]
  (start (setup)))

(comment
  (setup)
  (start)
  (stop))
