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
    [swag.core :only (http-codes)]
    [clojure.core.strint :only (<<)]
    ))

(defn import-logging []
   (use '[taoensso.timbre :only (debug info error warn trace)]))

(import-logging)

(defn get!
  "Reading a keys path from configuration raises an error of keys not found"
  [& keys] 
  (if-let [v (get-in config keys)]
    v
    (throw+ {:type ::missing-conf :message (<< "No matching configuration keys ~{keys} found")})))

(defn get* 
  "Quite version of get!"
  [& keys]
   (get-in config keys))

(defn slurp-edn [file] (read-string (slurp file)))

; basic time manipulation
(defn curr-time [] (.getTime (Date.)))

(def minute (* 1000 60))

(def half-hour (* minute 30))
 
; common api functions
(defn resp
  "Http resposnse compositor"
  [code data] {:status (http-codes code) :body data})

(def bad-req (partial resp :bad-req))
(def conflict (partial resp :conflict))
(def success (partial resp :success))
 
(defn gen-uuid [] (str (java.util.UUID/randomUUID)))

(defn interpulate
  "basic string interpulation"
  [text m]
  (clojure.string/replace text #"~\{\w+\}" 
    (fn [groups] ((keyword (subs groups 2 (dec (.length groups)))) m))))

(def version "0.0.10")
