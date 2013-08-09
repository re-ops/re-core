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

(ns proxmox.http-common
  (:require 
    [clojure.core.strint :refer (<<)]
    [celestial.common :refer (import-logging)]
    [proxmox.model :refer (proxmox-master)]))

(import-logging)

(defn root [] (<< "https://~((proxmox-master) :host):8006/api2/json"))

(defn retry
  "A retry for http calls against proxmox, some operations lock machine even after completion"
  [ex try-count _]
  (debug "re-trying due to" ex "attempt" try-count)
  (Thread/sleep 1000) 
  (if (> try-count 1) false true))

(def http-opts {:insecure? true :retry-handler retry })



