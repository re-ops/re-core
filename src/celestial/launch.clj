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
    [clojure.core.strint :only (<<)]
    [clojure.tools.nrepl.server :only (start-server stop-server)]
    [celestial.persistency :as p]
    [gelfino.timbre :only (set-tid get-tid)]
    [clojure.java.io :only (resource)]
    [gelfino.timbre :only (gelf-appender)]
    [supernal.core :only [ssh-config]]
    [celestial.common :only (get! get* import-logging version)]
    [taoensso.timbre :only (set-config! set-level!)])
  (:require 
    [components.core :refer (start-all stop-all setup-all)]
    [celestial.persistency.migrations :as mg]
    [hypervisors.networking :refer [initialize-networking]]
    [es.core :as es]
    [celestial.jobs :as jobs]
    [celestial.api.core :as api]))

(import-logging)

(defn setup-logging 
  "Sets up logging configuration"
  []
  (let [log* (partial get* :celestial :log)]
    (when (log* :gelf)
      (set-config! [:appenders :gelf] gelf-appender) 
      (set-config! [:shared-appender-config :gelf] {:host (log* :gelf :host)})) 
    (set-config! [:shared-appender-config :spit-filename] (log* :path)) 
    (set-config! [:appenders :spit :enabled?] true) 
    (set-level! (log* :level))))

(defn build-components []
  {:es (es/instance) :jobs (jobs/instance) :jetty (api/instance)})

(defn clean-up 
  "Clean/release resources, used also as a shutdown hook"
  [components]
  (debug "Shutting down...")
  (stop-all components))

(defn add-shutdown [components]
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (clean-up components)))))

(defn start-nrepl
  "Starts nrepl if enabled"
  []
  (when-let [port (get* :celestial :nrepl :port)]
    (info (<< "starting nrepl on port ~{port}"))
    (defonce server (start-server :port port))))

(defn setup 
  "One time setup" 
  []
  (let [components (build-components)]
    (initialize-networking)
    (setup-logging)
    (p/reset-admin)
    (mg/setup-migrations)
    (add-shutdown components)
    (ssh-config {:key (get! :ssh :private-key-path) :user "root"} ) 
    (start-nrepl)
    (setup-all components) 
     components)
  )

(defn start 
  "Main components startup (jetty, job workers etc..)"
  [components]
  (start-all components)
  (info (slurp (resource "main/resources/celestial.txt")))
  (info (<<  "version ~{version} see http://celestial-ops.com"))
  components
  )

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
