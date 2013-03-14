(ns celestial.launch
  "celestial lanching ground aka main"
  (:gen-class true)
  (:use 
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.common :only (config import-logging)]
    )
  (:require [celestial.jobs :as jobs]))

(import-logging)

(defn add-shutdown []
  (.addShutdownHook (Runtime/getRuntime) 
                    (Thread. 
                      (fn [] 
                        (debug "Shutting down...")
                        (jobs/shutdown-workers)))) )
(defn -main [& args]
  (add-shutdown)
  (jobs/clear-all)
  (jobs/initialize-workers)
  (run-jetty app  {:port (get-in config [:celestial :port]) :join? true}))
