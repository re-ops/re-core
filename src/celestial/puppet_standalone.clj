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

(ns celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:use 
    clojure.pprint
    [clj-yaml.core :as yaml]
    [clojure.java.io :only (file)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Provision)]
    [celestial.model :only (pconstruct)]
    [taoensso.timbre :only (debug info error warn)]
    [supernal.core :only (ns- lifecycle copy run execute env)]
    [clojure.string :only (join)]))

(defn copy-module [remote {:keys [src name]}]
  {:pre [(remote :host) src name]}
  "Copy a opsk module into server"
  )

(env {})

(defn as-root [{:keys [user] :as remote} cmd]
  (if (and user (not= user "root"))
    (<< "sudo ~{cmd}") cmd))

(defn args-from [{:keys [args]}]
   (or (some->> args (join " ")) ""))

(ns- puppet
   (task copy-module
     (let [{:keys [module]} args]
       (copy (module :src) "/tmp" (or (module :options) {})) ))

   (task extract-module 
      (let [{:keys [module]} args]
       (run (<< " cd /tmp && tar -xzf ~(module :name).tar.gz"))))
    
   (task copy-yaml
     (let [{:keys [hostname module classes]} args path (<< "/tmp/~{hostname}.yml") f (file path) ]
       (info args)
       (spit f (yaml/generate-string {:classes classes}))
       (debug "copy from " path remote)
       (copy path (<< "/tmp/~(module :name)/"))
       (.delete f))) 
   
   (task run-puppet
      (let [{:keys [module]} args]
        (run (str (<< "cd /tmp/~(module :name)") " && " 
          (as-root remote (<< "./scripts/run.sh ~(args-from args) --detailed-exitcodes || [ $? -eq 2 ]"))))))

   (task cleanup-tmp
      (let [{:keys [module]} args]
        (run (<< "rm -rf /tmp/~(module :name)*"))))) 

(lifecycle cleanup {:doc "puppet standalone cleanup"}
   {puppet/cleanup-tmp #{}})

(lifecycle puppet-provision {:doc "basic puppet standalone provisioning" :failure cleanup}
  {puppet/copy-module #{puppet/extract-module}
   puppet/extract-module #{puppet/copy-yaml}
   puppet/copy-yaml #{puppet/run-puppet}
   puppet/run-puppet #{puppet/cleanup-tmp}
   })

(defrecord Standalone [remote m]
  Provision
  (apply- [this]
    (let [roles {:roles {:web #{remote}}}
          res (execute puppet-provision m :web :env roles)]
        (when-let [fail (-> res first :fail)] 
          (throw fail))
      ))) 


(defmethod pconstruct :puppet-std [type {:keys [machine env] :as spec}]
  (let [remote {:host (machine :ip) :user (or (machine :user) "root")}
        by-env (get-in type [:puppet-std env])]
    (Standalone. remote (assoc by-env :hostname (machine :hostname)))))
