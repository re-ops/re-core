(ns celestial.launch
  "celestial lanching ground aka main"
  (:gen-class true)
  (:use 
    [celestial.api :only (app)]
    [gelfino.timbre :only (gelf-appender)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.common :only (get* import-logging)]
    [celestial.redis :only (clear-locks)]
    [taoensso.timbre :only (set-config! set-level!)])
  (:require [celestial.jobs :as jobs]))

(import-logging)

(def log* (partial get* :celestial :log))

(set-config! [:appenders :gelf] gelf-appender)
(set-config! [:shared-appender-config :gelf] {:host (log* :gelf-host)})
 
(set-config! [:shared-appender-config :spit-filename] (log* :path))
(set-config! [:appenders :spit :enabled?] true)
(set-level! (log* :level))

(defn clean-up []
  (debug "Shutting down...")
  (jobs/shutdown-workers)
  (jobs/clear-all)
  (clear-locks))

(defn add-shutdown []
  (.addShutdownHook (Runtime/getRuntime) (Thread. clean-up)))

(defn -main [& args]
  (add-shutdown)
  (jobs/initialize-workers)
  (run-jetty (app true)  {:port (get* :celestial :port) :join? true}))
