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

(ns celestial.hosts-api
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p]) 
  (:use 
     [clojure.core.strint :only (<<)]
     [slingshot.slingshot :only  [throw+ try+]]
     [swag.model :only (defmodel wrap-swag defv defc)]
     [celestial.common :only (import-logging resp bad-req conflict success wrap-errors)]
     [swag.core :only (swagger-routes set-base GET- POST- PUT- DELETE- defroutes- errors)]))

(import-logging)

(defmodel type :type :string :puppet-std {:type "Puppetstd"} :classes {:type "Object"})

(defmodel puppetstd :module {:type "Module"} :args {:type "List"})

(defmodel module :name :string :src :string)

(defmodel system 
  :env :string
  :machine {:type "Machine"} 
  :aws {:type "Aws" :description "An EC2 based system"}
  :proxmox {:type "Proxmox" :description "A Proxmox based system"}
  :vcenter {:type "Vcenter" :description "A vCenter based system"}
  :type :string)

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

(defmodel aws :instance-type :string :image-id :string :key-name :string :endpoint :string)

(defmodel vcenter :pool :string :datacenter :string :hostsystem :string :disk-format :string)

(defmodel proxmox :nameserver :string :searchdomain :string :password :string :node :string 
  :type {:type :string :allowableValues {:valueType "LIST" :values ["ct" "vm"]}}
  :features {:type "List"})

(defc [:proxmox :type] (keyword v))

(defc [:machine :os] (keyword v))

(defc [:env] (keyword v))

(set-base "")

(defn systems-range
  "Get systems in range by type" 
  [from to type]
  {:pre [(> from -1) (if type (p/type-exists! type) true)]}
  (let [systems (if type (p/get-system-index :type type) (into [] (p/all-systems)))
        to* (min to (count systems))]
    (println to* (contains? systems to*))
    (when-not (empty? systems)
      (if (and (contains? systems from) (contains? systems (- to* 1)))
        {:meta {:total (count systems)} :systems (map (juxt identity p/get-system) (subvec systems from to*))} 
        (throw+ {:type ::non-legal-range :message (<<  "No legal systems in range ~{from}:~{to*} try between ~{0}:~(count systems)")})))))



(defroutes- system {:path "/host" :description "Operations on hosts"}

  (GET- "/systems" [^:int page ^:int offset ^:string type] 
      {:nickname "getSystems" :summary "Get all systems at page with offset"}
    (let [page* (Integer/valueOf page) offset* (Integer/valueOf offset)]
      (success (systems-range (* (- page* 1) offset*) (* page*  offset*) type))))

  (GET- "/systems/:id" [^:int id] {:nickname "getSystem" :summary "Get system by id"}
        (success (p/get-system id)))

  (GET- "/system-by/:type" [^:string type] {:nickname "getSystemsByType" :summary "Get systems by type"}
        (success {:ids (p/get-system-index :type type)}))

  (POST- "/system" [& ^:system spec] {:nickname "addSystem" :summary "Add system" 
                                           :errorResponses (errors {:bad-req "Missing system type"})}
         (wrap-errors
           (p/with-quota (p/add-system spec) spec
             (success {:msg "new system saved" :id id}))))

  (POST- "/system-clone/:id/:hostname" [^:int id ^:string hostname] 
         {:nickname "cloneSystem" :summary "Clone an existing system " 
          :notes "Clones a system replacing unique identifiers along the way,
                 the only user provided value is the dest hostname"
           :errorResponses (errors {:bad-req "System missing"})}
         (if (p/system-exists? id)
           (p/with-quota 
             (p/clone-system id hostname) (p/get-system id)
             (success {:msg "system cloned" :id id}))
           (conflict {:msg "System does not exists, use POST /host/system to create it first"})))

  (PUT- "/systems/:id" [^:int id & ^:system system] {:nickname "updateSystem" :summary "Update system" 
                                                         :errorResponses (errors {:conflict "System does not exist"}) }
        (if-not (p/system-exists? id)
          (conflict {:msg "System does not exists, use POST /host/system first"}) 
          (wrap-errors
            (p/update-system id system) 
            (success {:msg "system updated" :id id}))))

  (DELETE- "/systems/:id" [^:int id] {:nickname "deleteSystem" :summary "Delete System" 
                                          :errorResponses (errors {:bad-req "System does not exist"})}
           (try+ 
             (let [spec (p/get-system! id) int-id (Integer/valueOf id)]               
               (p/delete-system! id) 
               (p/decrease-use int-id spec)
               (success {:msg "System deleted"})) 
             (catch [:type :celestial.persistency/missing-system] e 
               (bad-req {:msg "System does not exist"}))))

  (GET- "/systems/:id/type" [^:int id] {:nickname "getSystemType" :summary "Fetch type of provided system id"}
        (success (p/get-type (:type (p/get-system id))))) 
  )


(defroutes- type {:path "/type" :description "Operations on types"}

  (GET- "/types/:type" [^:string type] {:nickname "getType" :summary "Get type"}
        (success (p/get-type type)))

  (POST- "/types" [& ^:type props] {:nickname "addType" :summary "Add type"}
         (wrap-errors 
           (p/add-type props)
           (success {:msg "new type saved"})))

  (PUT- "/types" [& ^:type props] {:nickname "updateType" :summary "Update type"}
        (wrap-errors
          (if-not (p/type-exists? (props :type))
            (conflict {:msg "Type does not exists, use POST /type first"}) 
            (do (p/update-type props) 
                (success {:msg "type updated"})))))

  (DELETE- "/types/:type" [^:string type] {:nickname "deleteType" :summary "Delete type" 
                                          :errorResponses (errors {:bad-req "Type does not exist"})}
           (if (p/type-exists? type)
             (do (p/delete-type type) 
                 (success {:msg "Type deleted"}))
             (bad-req {:msg "Type does not exist"}))) 
  )

