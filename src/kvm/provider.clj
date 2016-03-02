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

(ns kvm.provider
  (:require 
    [clojure.core.strint :refer (<<)] 
    [kvm.clone :refer (clone-domain)]
    [kvm.common :refer (connect)]
    [celestial.provider :refer (wait-for wait-for-ssh map-key)] 
    [supernal.sshj :refer (ssh-up?)]
    [celestial.core :refer (Vm)] 
    [taoensso.timbre :as timbre]
    [celestial.persistency.systems :as s]
    [celestial.model :refer (translate vconstruct)]))


(timbre/refer-timbre)

(defn connection [{:keys [host user port]}] 
  (connect (<< "qemu+ssh://~{user}@~{host}:~{port}/system")))

(defmacro with-connection [& body]
  `(let [~'connection (connection ~'node)] (do ~@body)))

(defrecord Domain [node domain]
  Vm
  (create [this]
    (with-connection 
      (let [{:keys [image target]} domain]
        (clone-domain connection image target)))) 

  (delete [this])

  (start [this])

  (stop [this])

  (status [this])

  (ip [this]))

(defmethod translate :kvm [{:keys [machine digital-ocean system-id] :as spec}] 
   
  )

(defmethod vconstruct :kvm [{:keys [digital-ocean machine] :as spec}]

  )

