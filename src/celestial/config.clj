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
  (:require 
    [subs.core :refer (validate! combine validation when-not-nil every-kv)])
  (:use 
    [clojure.pprint :only (pprint)]
    [taoensso.timbre :only (set-config! set-level! debug info error warn trace)]
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [clj-config.core :as conf])
  )


(def base-v 
  {:redis {:host #{:required :String}} :ssh {:private-key-path #{:required :String}}})

(def levels #{:trace :debug :info :error})

(validation :levels
  (when-not-nil levels (<< "level must be either ~{levels}")))

(def central-logging #{:graylog2 :kibana :logstash})

(validation :central-logging
  (when-not-nil central-logging (<< "type must be either ~{central-logging}")))
 
(def ^{:doc "Base config validation"} celestial-v
  {:celestial
   {:port #{:required :number} :https-port #{:required :number}
    :log {
       :level #{:required :levels} 
       :path #{:required :String}
       :gelf {
          :host #{:String} :type #{:central-logging}
        }
    } 
    :cert {:password #{:required :String} :keystore #{:required :String}} 
    :job {:expiry #{:number} :wait-time #{:number}} 
    :nrepl {:port #{:number}}}})

(validation :node*
   (every-kv {:username #{:required :String} :password #{:required :String} 
              :host #{:required :String} :ssh-port #{:required :number}}))

(def flavors #{:redhat :debian})

(validation :flavor
   (when-not-nil flavors  (<< "flavor must be either ~{flavors}")))

(validation :template*
  (every-kv {:template #{:required :String} :flavor #{:required :Keyword :flavor}}))


(def ^{:doc "proxmox section validation"} proxmox-v 
  {:proxmox { 
     :master #{:required :Keyword} :nodes #{:required :node*} 
     :ostemplates #{:template*}}})

(def ^{:doc "aws section validation"} aws-v 
  {:aws {:access-key #{:required :String} :secret-key #{:required :String}}})

(def ^{:doc "vcenter section validation"} vcenter-v 
  {:vcenter {
      :url #{:required :String} :username #{:required :String}
      :password #{:required :String} :session-count #{:required :number}
      :ostemplates #{:required :Map} }})

(defn validate-conf 
  "applies all validations on a configration map"
  [{:keys [hypervisor] :as c}]
  (let [hvs [proxmox-v aws-v vcenter-v] ks (map (comp first keys) hvs) 
        envs (map (fn [v] (fn [env] {:hypervisor {env v}})) hvs)]
    (validate! c 
      (apply combine (into [base-v celestial-v] 
        (first (map (fn [[e hs]] (map #(((zipmap ks envs) %) e) (filter (into #{} ks) (keys hs)))) hypervisor)))))))

(def config-paths
  ["/etc/celestial/celestial.edn" (<< "~(System/getProperty \"user.home\")/.celestial.edn")])

(def path 
  (first (filter #(.exists (file %)) config-paths)))

(defn pretty-error 
  "A pretty print error log"
  [m c]
  (let [st (java.io.StringWriter.)]
    (binding [*out* st] 
      (clojure.pprint/pprint m))
    (set-config! [:shared-appender-config :spit-filename] 
                 (get-in c [:celestial :log :path] "celestial.log")) 
    (set-config! [:appenders :spit :enabled?] true) 
    (error "Following configuration errors found:\n" st))) 

(defn read-and-validate []
  (let [c (conf/read-config path) es (validate-conf c)]
    (when-not (empty? es) 
      (pretty-error es c)
      (System/exit 1))
    c))


(def ^{:doc "main configuation"} config 
  (if path
    (read-and-validate)      
    (when-not (System/getProperty "disable-conf") 
      (error 
        (<< "Missing configuration file, you should configure celestial in either ~{config-paths}"))  
      (System/exit 1))))

