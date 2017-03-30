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

(ns re-core.persistency.core
  (:require
    [re-core.common :refer (import-logging)]
    [re-core.redis :refer (server-conn)]
    [components.core :refer (Lifecyle)]
    [puny.redis :as r]))

(import-logging)

(defn initilize-puny
   "Initlizes puny connection"
   []
  (info "Initializing puny connection" (server-conn))
  (r/server-conn (server-conn)))

(defrecord Persistency
  []
  Lifecyle
  (setup [this]
    (initilize-puny))
  (start [this])
  (stop [this])
  )

(defn instance
   "creats a jobs instance"
   []
  (Persistency.))
