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

(ns kvm.provider
  (:require 
    [safely.core :refer [safely]]
    [kvm.validations :refer (provider-validation)]
    [clojure.core.strint :refer (<<)] 
    [kvm.clone :refer (clone-domain)]
    [kvm.disks :refer (clear-volumes)]
    [kvm.common :refer (connect get-domain domain-zip state)]
    [kvm.networking :refer (public-ip)]
    [re-mote.sshj :refer (ssh-up?)]
    [re-core.core :refer (Vm)] 
    [taoensso.timbre :as timbre]
    [re-core.persistency.systems :as s]
    [re-core.provider :refer (mappings selections transform os->template wait-for wait-for-ssh)]
    [hypervisors.networking :refer (set-hostname)]
    [re-core.model :refer (translate vconstruct hypervisor*)])
  (:import org.libvirt.LibvirtException))

(timbre/refer-timbre)

(defn connection [{:keys [host user port]}] 
    (safely
       (connect (<< "qemu+ssh://~{user}@~{host}:~{port}/system"))
       :on-error
       :log-errors false
       :max-retry 5
       :message "Error while trying to connect to libvirt"
       :retry-delay [:random-range :min 200 :max 500]))

(defmacro with-connection [& body]
  `(let [~'connection (connection ~'node)] 
      (do ~@body)))

(defn wait-for-status [instance req-stat timeout]
  "Waiting for ec2 machine status timeout is in mili"
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
    {:type ::kvm:status-failed :status req-stat :timeout timeout} 
      "Timed out on waiting for status"))


(defrecord Domain [system-id node domain]
  Vm
  (create [this]
    (with-connection 
      (let [image (get-in domain [:image :template]) target (select-keys domain [:name :cpu :ram])]
        (clone-domain connection image target)
        (debug "clone done")
        (wait-for-status this "running" [5 :minute])
        (debug "in running state")
        (let [ip (.ip this)]
          (wait-for-ssh ip (domain :user) [5 :minute])
          (set-hostname (domain :hostname) (domain :name) {:user (domain :user) :host ip} (get-in domain [:image :flavor]))
          this)))) 

  (delete [this]
    (with-connection 
      (clear-volumes connection (domain-zip connection (domain :name)))
      (.undefine (get-domain connection (domain :name))))
    )

  (start [this]
    (with-connection 
      (when-not (= (.status this) "running")
        (.create (get-domain connection (domain :name)))
        (wait-for-ssh (.ip this) (domain :user) [5 :minute])
        )))

  (stop [this]
    (with-connection 
      (.destroy (get-domain connection (domain :name)))
      (wait-for-status this "shutoff" [5 :minute])
      ))

  (status [this]
    (with-connection 
      (try 
        (state (get-domain connection (domain :name)))
          (catch LibvirtException e (debug (.getMessage e)) false))))

  (ip [this]
    (with-connection 
      (let [ip  (public-ip connection (domain :user) node (domain :name))]
        (debug "domain public ip is" ip) ip)))
  
  )

(defn machine-ts 
  "Construcuting machine transformations"
  [{:keys [hostname domain] :as machine}]
   {:name (fn [hostname] (<< "~{hostname}.~{domain}")) 
    :image (fn [os] ((os->template :kvm) os))})

(defmethod translate :kvm [{:keys [machine kvm] :as spec}] 
   (-> machine
     (mappings {:os :image :hostname :name})
     (transform (machine-ts machine))
     (assoc :hostname (machine :hostname)) 
     (selections [[:name :user :image :cpu :ram :hostname]])
     ))

(defmethod vconstruct :kvm [{:keys [kvm machine system-id] :as spec}]
   (let [[domain] (translate spec) {:keys [node]} kvm
         node* (mappings (hypervisor* :kvm :nodes node) {:username :user})]
     (provider-validation domain node*)
     (->Domain system-id node* domain)))

