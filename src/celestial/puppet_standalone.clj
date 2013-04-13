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
    [supernal.sshj :only (copy batch execute)]
    [taoensso.timbre :only (debug info error warn)]
    ))


(defn copy-module [remote {:keys [src name]}]
  {:pre [(remote :host) src name]}
  "Copy a opsk module into server"
  (copy src "/tmp" remote ))

(defn copy-yml-type [{:keys [type hostname puppet-std] :as _type} remote]
  (let [path (<< "/tmp/~{hostname}.yml") f (file path)
        name (get-in puppet-std [:module :name])]
    (spit f (yaml/generate-string (select-keys _type [:classes])))
    (copy path (<< "/tmp/~{name}/") remote )
    (.delete f)))

(defn as-root [remote cmd]
   (if (remote :user)
     (<< "sudo ~{cmd}") cmd))

(defrecord Standalone [remote type]
  Provision
  (apply- [this]
    (let [puppet-std (type :puppet-std) module (puppet-std :module) sudo ()]
     (try 
      (copy-module remote module) 
      (execute (<< " cd /tmp && tar -xzf ~(:name module).tar.gz") remote) 
      (copy-yml-type type remote)
      (execute (str (<< "cd /tmp/~(:name module)") " && " (as-root remote "./scripts/run.sh")) remote)
      (finally 
        #_(execute remote (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*")))))))) 


(defmethod pconstruct :puppet-std [type {:keys [machine] :as spec}]
  (let [remote {:host (or (machine :ip) (machine :ssh-host)) :user (or (machine :user) "root")}]
    (Standalone. remote (assoc type :hostname (machine :hostname)))))
