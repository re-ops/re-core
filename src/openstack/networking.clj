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

(ns openstack.networking
  "openstack networking management"
  (:require
    [re-core.common :refer (import-logging)]
    [clojure.java.data :refer [from-java]]
    [re-core.persistency.systems :as s]))

(import-logging)

(defn addresses-ip
   "grab addresses from server" 
   [server network]
   (map (comp :addr from-java) (get-in (from-java server) [:addresses :addresses network])))

(defn first-ip [server network] (first (addresses-ip server network)))

(defn update-ip [spec ip]
  "update instance ip"
  (when (s/system-exists? (spec :system-id))
     (s/partial-system (spec :system-id) {:machine {:ip ip}})))

(defn update-floating [spec ip]
  "update instance ip"
  (when (s/system-exists? (spec :system-id))
     (s/partial-system (spec :system-id) {:openstack {:floating-ip ip}})))

(defn assoc-floating 
   "assoc floating ip with instance"
   [floating-ips server ip]
     (debug "assoc" ip "to" (:id (from-java server)))
     (.addFloatingIP floating-ips server ip))

(defn allocate-floating 
   "allocating floating ip using pool"
   [floating-ips pool]
     (.getFloatingIpAddress (.allocateIP floating-ips pool)))

(defn dissoc-floating
   "dissoc ip for instance" 
   [floating-ips server ip]
     (debug "dissoc" ip "from" (:id (from-java server)))
     (.removeFloatingIP floating-ips server ip))
  
