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

(ns celestial.api.systems
  "Celestial systems api"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency [types :as t] [users :as u]]
    [celestial.security :refer (current-user)]
    [ring.util.codec :refer (base64-decode)]
    [clojure.data.json :as json]
    [cemerick.friend :as friend]
    [celestial.persistency.systems :as s]
    [es.systems :as es]
    [celestial.model :refer (sanitized-envs)] 
    [clojure.core.strint :refer (<<)] 
    [slingshot.slingshot :refer  [throw+ try+]] 
    [swag.model :refer (defmodel defc)] 
    [celestial.common :refer (import-logging bad-req conflict success wrap-errors)] 
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes- errors)]))

(import-logging)

(defmodel system 
  :env :string
  :type :string
  :user :string
  :machine {:type "Machine"} 
  :aws {:type "Aws" :description "An EC2 based system"}
  :physical {:type "Physical" :description "A physical machine"}
  :proxmox {:type "Proxmox" :description "A Proxmox based system"}
  :vcenter {:type "Vcenter" :description "A vCenter based system"})

(defmodel machine 
  :cpus {:type :int :description "Not relevant in ec2"}
  :memory {:type :int :description "Not relevant in ec2"}
  :disk {:type :int :description "Not relevant in ec2"}
  :user :string :os {:type :string :description "OS template used"}
  :hostname :string 
  :domain {:type :string :description "dns domain"}
  :ip {:type :string :description "Not relevant in ec2"}
  :netmask {:type :string :description "used in vCenter or proxmox bridge"}
  :gateway {:type :string :description "used vCenter or proxmox bridge"})

(defmodel physical :mac :string)

(defmodel aws :instance-type :string :key-name :string :endpoint :string)

(defmodel vcenter :pool :string :datacenter :string :hostsystem :string :disk-format :string)

(defmodel proxmox :nameserver :string :searchdomain :string :password :string :node :string 
  :type {:type :string :allowableValues {:valueType "LIST" :values ["ct" "vm"]}}
  :features {:type "List"})

(defmodel query :must {:type "List"} :should {:type "List"} :must_not {:type "List"})

(defc "/systems" [:proxmox :type] (keyword v))

(defc "/systems" [:machine :os] (keyword v))

(defc "/systems" [:env] (keyword v))
 
(defn working-username  []
   (let [{:keys [username]} (current-user) ] 
     username))

(defn systems-range
  "Get systems in range" 
  [from to]
  {:pre [(> from -1)]}
  (let [systems (into [] (s/systems-for (working-username))) to* (min to (count systems))]
    (when-not (empty? systems)
      (if (and (contains? systems from) (contains? systems (- to* 1)))
        {:meta {:total (count systems)} 
         :systems (doall (map (juxt identity s/get-system) (subvec systems from to*)))} 
        (throw+ {:type ::non-legal-range :message (<<  "No legal systems in range ~{from}:~{to*} try between ~{0}:~(count systems)")})))))

(defroutes- systems {:path "/systems" :description "Operations on Systems"}

  (GET- "/systems" [^:int page ^:int offset]
      {:nickname "getSystems" :summary "Get all systems at page with offset"}
    (let [page* (Integer/valueOf page) offset* (Integer/valueOf offset)]
      (success (systems-range (* (- page* 1) offset*) (* page*  offset*)))))

  (GET- "/systems/query" [^:int page ^:int offset ^:query query]
      {:nickname "getSystemsBy" :summary "Get all systems at page with offset by query"}
    (let [page* (Integer/valueOf page) offset* (Integer/valueOf offset) 
          query-m (json/read-str (String. (base64-decode query)) :key-fn keyword)
          {:keys [hits]} (es/systems-for (working-username) {:bool query-m} (* (- page* 1) offset*)  offset*)]
      (success 
        {:meta {:total (:total hits)} 
         :systems (doall (map (juxt identity s/get-system) (map :_id (:hits hits))))})))

  (GET- "/systems/:id" [^:int id] {:nickname "getSystem" :summary "Get system by id"}
        (success (s/get-system id)))

  (GET- "/systems/type/:type" [^:string type] {:nickname "getSystemsByType" :summary "Get systems by type"}
        (success {:ids (s/get-system-index :type type)}))

  (POST- "/systems" [& ^:system spec] 
    {:nickname "addSystem" :summary "Add system" 
     :errorResponses (errors {:bad-req "Missing system type"})}
         (wrap-errors
           (let [id (s/add-system spec)]
             (success {:message "new system saved" :id id}))))

  (PUT- "/systems/:id" [^:int id & ^:system system] {:nickname "updateSystem" :summary "Update system" 
                                                         :errorResponses (errors {:conflict "System does not exist"}) }
        (if-not (s/system-exists? id)
          (conflict {:message "System does not exists, use POST /host/system first"}) 
          (wrap-errors
            (s/update-system id system) 
            (success {:message "system updated" :id id}))))

  (DELETE- "/systems/:id" [^:int id] {:nickname "deleteSystem" :summary "Delete System" 
                                          :errorResponses (errors {:bad-req "System does not exist"})}
         (try+ 
             (let [spec (s/get-system! id) int-id (Integer/valueOf id)]               
               (s/delete-system! id) 
               (success {:message "System deleted"})) 
             (catch [:type :celestial.persistency/missing-system] e 
               (bad-req {:message "System does not exist"}))))

  (GET- "/systems/:id/type" [^:int id] {:nickname "getSystemType" :summary "Fetch type of provided system id"}
        (success (t/get-type (:type (s/get-system id))))))

(defroutes- environments {:path "/environments" :description "Operations on environments"}
  (GET- "/environments" [] {:nickname "getEnvironments" :summary "Get all environments"}
     (let [{:keys [envs] :as user} (u/get-user (working-username))]
        (success {:environments (sanitized-envs (into #{} envs))}))))
