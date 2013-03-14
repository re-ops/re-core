(ns celestial.launch
  "celestial lanching ground aka main"
  (:gen-class true)
  (:use 
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.common :only (config import-logging)]
    [celestial.redis :only (clear-locks)]
    )
  (:require [celestial.jobs :as jobs]))

(import-logging)

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
  (run-jetty app  {:port (get-in config [:celestial :port]) :join? true}))
