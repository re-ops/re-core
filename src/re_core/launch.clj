(comment
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns re-core.launch
  "re-core lanching ground aka main"
  (:gen-class true)
  (:require
    [clojure.tools.nrepl.server :refer (start-server stop-server)]
    [clojure.java.io :refer (resource)]
    [re-core.common :refer (get! get* version)]
    [clojure.core.strint :refer (<<)]
    [gelfino.timbre :refer (set-tid get-tid gelf-appender)]
    [taoensso.timbre  :as timbre :refer (merge-config! set-level!)]
    [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender) ]
    [taoensso.timbre.appenders.core :refer (println-appender)]
    [components.core :refer (start-all stop-all setup-all)]
    [re-core.persistency.core :as p]
    [hypervisors.networking :refer (initialize-networking)]
    [es.core :as es]
    [re-core.metrics :as met]
    [re-core.jobs :as jobs]
    [re-core.schedule :as sch]
    [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn disable-coloring
   "See https://github.com/ptaoussanis/timbre"
   []
  (merge-config! {:output-fn (partial timbre/default-output-fn  {:stacktrace-fonts {}})}))

(defn setup-logging
  "Sets up logging configuration"
  []
  (let [log* (partial get* :re-core :log)]
    (when (log* :gelf)
      (merge-config!
        {:appenders {:gelf (gelf-appender {:host (log* :gelf :host)})}}))
    (merge-config!
      {:appenders {:rolling (rolling-appender {:path (log* :path) :pattern :weekly})}})
    (merge-config!
      {:appenders {:println {:ns-whitelist ["re-core"]}}})
    (disable-coloring)
    (set-level! (log* :level))))

(defn build-components []
  {:es (es/instance) :jobs (jobs/instance)
   :metrics (met/instance)
   :persistency (p/instance) :schedule (sch/instance)})

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
  (when-let [port (get* :re-core :nrepl :port)]
    (info (<< "starting nrepl on port ~{port}"))
    (defonce server (start-server :port port))))

(defn setup
  "One time setup"
  []
  (let [components (build-components)]
    (initialize-networking)
    (setup-logging)
    (add-shutdown components)
    (setup-all components)
     components))

(defn start
  "Main components startup (jetty, job workers etc..)"
  [components]
  (start-all components)
  (info (<<  "version ~{version} see https://github.com/re-ops/re-core"))
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
