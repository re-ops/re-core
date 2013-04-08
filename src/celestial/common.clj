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

(ns celestial.common
  (:import java.util.Date)
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.config :only (config)]
    [clojure.core.strint :only (<<)]
    ))

(defn import-logging []
   (use '[taoensso.timbre :only (debug info error warn trace)]))

(import-logging)

(defn get* 
  "Reading a keys path from configuration"
  [& keys] 
  (if-let [v (get-in config keys)]
    v
    (throw+ {:type ::missing-conf :message (<< "No matching configuration keys ~{keys} found")})))

(defn slurp-edn [file] (read-string (slurp file)))

(defn curr-time [] (.getTime (Date.)))
