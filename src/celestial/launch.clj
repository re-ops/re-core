(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

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
  (run-jetty (app true)  {:port (get* :celestial :port) :join? true 
                           :ssl? true :keystore "my.keystore" :key-password "foobar" :ssl-port 8443}))
