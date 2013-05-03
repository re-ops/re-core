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

(ns celestial.config
  "Celetial configuration info"
  (:require [celestial.validations :as cv])
  (:use 
    [celestial.validations :only (validate-nest)]
    [clojure.pprint :only (pprint)]
    [bouncer [core :as b] [validators :as v]]
    [bouncer.validators :only (defvalidator)]
    [taoensso.timbre :only (debug info error warn trace)]
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [clj-config.core :as conf])
  )

(def levels #{:trace :debug :info :error})

(defn base-v [c]
  (b/validate c 
    [:redis :host] [v/required cv/str?]
    [:ssh :private-key-path] [v/required cv/str?]))

(defn celestial-v
  "Base config validation"
  [c]
  (validate-nest c [:celestial]
    [:port] [v/required v/number]
    [:https-port] [v/required v/number]
    [:log :level] [v/required (v/member levels :message (<< "log level must be either ~{levels}"))]
    [:log :path] [v/required cv/str?]
    [:cert :password] [v/required cv/str?]
    [:cert :keystore] [v/required cv/str?] ))

(defn proxmox-v 
  "proxmox section validation"
  [c]
  (validate-nest c [:hypervisor :proxmox]
    [:username] [v/required cv/str?]
    [:password] [v/required cv/str?]
    [:host] [v/required cv/str?]
    [:ssh-port] [v/required v/number]))

(defn aws-v 
  "proxmox section validation"
  [c]
  (validate-nest c [:hypervisor :aws]
    [:access-key] [v/required cv/str?]
    [:secret-key] [v/required cv/str?]
    [:endpoint] [v/required cv/str?]))

(defn validate-conf 
  "applies all validations on a configration map"
  [c]
  (cond-> (-> c celestial-v second base-v second)
    (get-in c [:hypervisor :proxmox]) (-> proxmox-v second) 
    (get-in c [:hypervisor :aws])  (-> aws-v second)))

(def config-paths
  ["/etc/celestial.edn" (<< "~(System/getProperty \"user.home\")/.celestial.edn")])

(def path 
  (first (filter #(.exists (file %)) config-paths)))

(defn pretty-error 
  "A pretty print error log"
  [m]
  (let [st (java.io.StringWriter.)]
    (binding [*out* st] 
      (clojure.pprint/pprint m))
    (error "Following configuration errors found:\n" st))) 

(defn read-and-validate []
  (let [c (conf/read-config path) ]
    (when-let [v (:bouncer.core/errors (validate-conf c))] 
      (pretty-error v)
      (System/exit 1))
    c))


(def ^{:doc "main configuation"} config 
  (if path
    (read-and-validate)      
    (when-not (System/getProperty "disable-conf") 
      (error 
        (<< "Missing configuration file, you should configure celestial in either ~{config-paths}"))  
      (System/exit 1))))

