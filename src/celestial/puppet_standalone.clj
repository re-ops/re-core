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
    [clj-yaml.core :as yaml]
    [clojure.java.io :only (file)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Provision)]
    [celestial.model :only (pconstruct)]
    [taoensso.timbre :only (debug info error warn)]
    [supernal.core :only (ns- lifecycle copy run execute env)]
    ))

(defn copy-module [remote {:keys [src name]}]
  {:pre [(remote :host) src name]}
  "Copy a opsk module into server"
  )

(env {})

(defn as-root [remote cmd]
  (if (remote :user)
    (<< "sudo ~{cmd}") cmd))

(ns- puppet
   (task copy-module
     (let [{:keys [module]} args]
       (copy (module :src) "/tmp") ))

   (task extract-module 
      (let [{:keys [module]} args]
       (run (<< " cd /tmp && tar -xzf ~(:name module).tar.gz"))))
    
   (task copy-yaml
     (let [{:keys [type module]} args path (<< "/tmp/~(type :hostname).yml") f (file path) ]
       (spit f (yaml/generate-string (select-keys type [:classes])))
       (copy path (<< "/tmp/~(module :name)/"))
       (.delete f))) 
   
   (task run-puppet
      (let [{:keys [module]} args]
        (run (str (<< "cd /tmp/~(:name module)") " && " (as-root remote "./scripts/run.sh")))))

   (task cleanup
      (let [{:keys [module]} args]
        (run (<< "rm -rf /tmp/~(:name module)*"))))) 

(lifecycle puppet-provision {:doc "basic puppet standalone provisioning"}
  {puppet/copy-module #{puppet/extract-module}
   puppet/extract-module #{puppet/copy-yaml}
   puppet/copy-yaml #{puppet/run-puppet}
   puppet/run-puppet #{puppet/cleanup}
   })


(defrecord Standalone [remote type]
  Provision
  (apply- [this]
    (let [puppet-std (type :puppet-std) module (puppet-std :module)]
        (execute puppet-provision {:module module :type type} :web :join true :env {:roles {:web #{remote}}})
      ))) 


(defmethod pconstruct :puppet-std [type {:keys [machine] :as spec}]
  (let [remote {:host (or (machine :ip) (machine :ssh-host)) :user (or (machine :user) "root")}]
    (Standalone. remote (assoc type :hostname (machine :hostname)))))
