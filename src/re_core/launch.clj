(comment
   re-core, Copyright 2017 Ronen Narkis, narkisr.com
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
    [re-core.log :refer (setup-logging)]
    [re-core.common :refer (get! get* version)]
    [re-core.persistency.core :as p]
    [re-core.metrics :as met]
    [re-core.jobs :as jobs]
    [re-core.schedule :as sch] 
    [clojure.core.strint :refer (<<)]
    [clojure.java.io :refer (resource)]
    [components.core :refer (start-all stop-all setup-all)]
    [hypervisors.networking :refer (initialize-networking)]
    [es.core :as es]
    [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

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
