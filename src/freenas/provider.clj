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

(ns freenas.provider
  "Freenas jails provider"
  (:require 
    [supernal.core :refer (ns- lifecycle run execute env)]
    [taoensso.timbre :as timbre]
    [clojure.core.strint :refer (<<)] 
    [re-core.provider :refer (wait-for-ssh mappings wait-for)]
    [freenas.remote :refer [call]]
    [freenas.validations :refer [validate-provider jail-mappings]]
    [slingshot.slingshot :refer  [throw+]]
    [re-core.persistency.systems :as s]
    [org.httpkit.client :as client]
    [re-core.core :refer (Vm)] 
    [re-core.common :refer (get*)]
    [re-core.model :refer (hypervisor* hypervisor translate vconstruct)])
 )

(timbre/refer-timbre)

(defn instance-id*
  "grabbing instance id of spec"
   [spec]
  (get-in (s/get-system (spec :system-id)) [:freenas :id]))

(defmacro with-id [& body]
 `(if-let [~'id (instance-id* ~'spec)]
    (do ~@body) 
    (throw+ {:type ::freenas:missing-id} "Instance id not found"))
  )

(defn jails [host] 
  (let [js (call client/get "jails/jails/") ]
    (first (filter #(= (:jail_host %) host) js))))

(def jexec "/usr/sbin/jexec" )

(def sed "/usr/bin/sed")

(ns- jail
  (task enable-root-ssh
     (let [{:keys [host]} args]
       (run (<< "~{jexec} ~{host} ~{sed} -i.bak 's/^sshd_enable=.*/sshd_enable=\"YES\"/' /etc/rc.conf"))
       (run (<< "~{jexec} ~{host} ~{sed} -i.bak 's/^#PermitRootLogin no/PermitRootLogin yes/' /etc/ssh/sshd_config"))
       (run (<< "~{jexec} ~{host} /etc/rc.d/sshd start"))))

  (task add-ssh-key
    (let [{:keys [host]} args pub (clojure.string/trim-newline (slurp (get* :ssh :public-key-path)))]
      (run (<< "~{jexec} ~{host} mkdir /root/.ssh"))
      (run (<< "~{jexec} ~{host} sh -c  'pub=\"~{pub}\"; echo \"$pub\" | cat > /root/.ssh/authorized_keys'"))
      (run (<< "~{jexec} ~{host} chmod 700 /root/.ssh && chmod 600 /root/.ssh/authorized_keys")))))

(lifecycle prepare-jail {:doc "preparing jail with ssh and re-core key"}
   {jail/add-ssh-key #{} 
    jail/enable-root-ssh #{jail/add-ssh-key}})

(defn enable-ssh [host]
  (let [roles {:roles {:jails #{(hypervisor* :freenas)}}}]
     (execute prepare-jail {:host host} :jails :env roles)))

(defrecord Jail [spec]
  Vm
  (create [this] 
    (let [{:keys [jail_host]} (call client/post "jails/jails/" spec)
          {:keys [id]} (jails jail_host)]
     (debug "created" id)
     (s/partial-system (spec :system-id) {:freenas {:id id}})
     (enable-ssh jail_host)
      this))

  (start [this]
    (with-id
      (let [{:keys [machine]} (s/get-system (spec :system-id))]
        (when-not (= "running" (.status this))
          (call client/post (<< "jails/jails/~{id}/start") spec))
        (wait-for-ssh (machine :ip) "root" [5 :minute]))
      ))

  (delete [this]
     (with-id (call client/delete (<< "jails/jails/~{id}/"))))

  (stop [this]
    (with-id (call client/post (<< "jails/jails/~{id}/stop/"))))

  (status [this] 
    (when-let [jail (jails (spec :jail_host))]
      (.toLowerCase (:jail_status jail))))
  
  (ip [this]
     (get-in (s/get-system (spec :system-id)) [:machine :ip])))


(defmethod translate :freenas [{:keys [machine freenas system-id] :as spec}]
    "Convert the general model into a freenas jail"
    (mappings (merge machine freenas {:system-id system-id}) jail-mappings))

(defn validate [spec] 
  (validate-provider spec) spec)

(defmethod vconstruct :freenas [spec]
  (Jail. (validate (translate spec))))
