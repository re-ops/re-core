(ns celestial.launch
  "celestial lanching ground aka main"
  (:gen-class true)
  (:use 
    [celestial.api :only (app)]
    [celestial.logging :only (gelf-appender)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.common :only (config import-logging)]
    [celestial.redis :only (clear-locks)]
    [taoensso.timbre :only (set-config! set-level!)])
  (:require [celestial.jobs :as jobs]))

(import-logging)

(set-config! [:appenders :gelf] gelf-appender)
(set-config! [:shared-appender-config :gelf] {:host "192.168.5.9"})
 
(set-config! [:shared-appender-config :spit-filename ] "celestial.log"); TODO move this to /var/log
(set-config! [:appenders :spit :enabled?] true)
(set-level! :trace)

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
  (run-jetty (app true)  {:port (get-in config [:celestial :port]) :join? true}))
