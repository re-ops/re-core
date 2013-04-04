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
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (warn)]
    [clj-config.core :as conf]))

(def path 
  (first (filter #(.exists (file %))
    ["/etc/celestial.edn" (<< "~(System/getProperty \"user.home\")/.celestial.edn")])))

(def config 
   (if path
      (conf/read-config path)
      (do 
        (warn (<< "~{path} does not exist, make sure to configure it using default configuration")) 
        {:celestial 
          {:log {:level :trace :path "celestial.log" :gelf-host ""} }} 
        ) 
      ))

(defn get* [& keys]
  (get-in config keys))

(defn slurp-edn [file] (read-string (slurp file)))

(defn import-logging []
    (use '[taoensso.timbre :only (debug info error warn trace)]))

(defn curr-time [] (.getTime (Date.)))
