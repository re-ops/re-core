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

(ns re-core.log
  (:require 
    [re-core.common :refer (get! get* version)]
    [gelfino.timbre :refer (set-tid get-tid gelf-appender)]
    [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
    [taoensso.timbre.appenders.core :refer (println-appender)]
    [taoensso.timbre  :as timbre :refer (merge-config! set-level! refer-timbre)]))

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

(defn debug-on []
  (set-level! :debug))

(defn debug-off []
  (set-level! :info))
