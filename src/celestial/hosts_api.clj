(ns celestial.hosts-api
  (:refer-clojure :exclude [type])
  (:require [celestial.persistency :as p]) 
  (:use 
     [clojure.core.strint :only (<<)]
     [slingshot.slingshot :only  [throw+ try+]]
     [swag.model :only (defmodel wrap-swag defv defc)]
     [celestial.common :only (import-logging resp bad-req conflict success)]
     [swag.core :only (swagger-routes GET- POST- PUT- DELETE- defroutes- errors)])
 )

(import-logging)

(defmodel type :type :string :puppet-std {:type "Puppetstd"} :classes {:type "Object"})

(defmodel puppetstd :module {:type "Module"})

(defmodel module :name :string :src :string)

(defmodel system 
  :machine {:type "Machine"} 
  :aws {:type "Aws" :description "Only for ec2"}
  :proxmox {:type "Proxmox" :description "Only for proxmox"}
  :type :string)

(defmodel machine 
  :cpus {:type :int :description "Not relevant in ec2"}
  :memory {:type :int :description "Not relevant in ec2"}
  :disk {:type :int :description "Not relevant in ec2"}
  :hostname :string :user :string :os :string :ip {:type :string :description "Not relevant in ec2"})

(defmodel proxmox :nameserver :string :searchdomain :string :password :string :node :string 
  :type {:type :string :allowableValues {:valueType "LIST" :values ["ct" "vm"]}}
  :features {:type "List"})

(defv [:proxmox :type]
  (let [allowed (get-in proxmox [:properties :type :allowableValues :values])]
    (when-not (first (filter #{v} allowed))
      (throw (clojure.lang.ExceptionInfo. (<< "Value ~{v} for proxmox type isn't valid") {:error :validation})))))

(defc [:proxmox :type] (keyword v))

(defc [:machine :os] (keyword v))
 

(defroutes- hosts {:path "/host" :description "Operations on hosts"}

  (GET- "/host/system/:id" [^:int id] {:nickname "getSystem" :summary "Get system by id"}
        (success (p/get-system id)))

  (GET- "/host/system-by/:type" [^:string type] {:nickname "getSystemsByType" 
                                               :summary "Get systems by type"}

        (success {:ids (p/get-system-index :type type)}))

  (POST- "/host/system" [& ^:system props] {:nickname "addSystem" :summary "Add system" 
                                            :errorResponses (errors {:bad-req "Missing system type"})}
         (try+ 
           (let [id (p/add-system props)]
             (success {:msg "new system saved" :id id :props props})) 
           (catch [:type :celestial.persistency/missing-type] e 
             (bad-req {:msg (<< "Cannot create machine with missing type ~(e :t)}")}))))

  (POST- "/host/system-clone/:id" [^:int id] {:nickname "cloneSystem" :summary "Clone an existing system replacing unique identifiers along the way" 
                                         :errorResponses (errors {:bad-req "System missing"})}
         (if-not (p/system-exists? id)
           (conflict {:msg "System does not exists, use POST /host/system to create it first"}) 
           (let [clone-id (p/clone-system id)]  
             (success {:msg "system cloned" :id clone-id}))))

  (PUT- "/host/system/:id" [^:int id & ^:system system] {:nickname "updateSystem" :summary "Update system" 
                                                         :errorResponses (errors {:conflict "System does not exist"}) }
        (if-not (p/system-exists? id)
          (conflict {:msg "System does not exists, use POST /host/system first"}) 
          (do (p/update-system id system) 
              (success {:msg "system updated" :id id}))))

  (DELETE- "/host/system/:id" [^:int id] {:nickname "deleteSystem" :summary "Delete System" 
                                          :errorResponses (errors {:bad-req "System does not exist"})}
           (if (p/system-exists? id)
             (do (p/delete-system id) 
                 (success {:msg "System deleted"}))
             (bad-req {:msg "System does not exist"})))

  (GET- "/host/type/:id" [^:int id] {:nickname "getSystemType" :summary "Fetch type of provided system id"}
        (success (p/get-type (:type (p/get-system id))) ))

  )


(defroutes- types {:path "/type" :description "Operations on types"}

  (GET- "/type/:type" [^:string type] {:nickname "getType" :summary "Get type"}
        (success (p/get-type type)))

  (POST- "/type" [& ^:type props] {:nickname "addType" :summary "Add type"}
         (p/add-type props)
         (success {:msg "new type saved" :type props}))

  (PUT- "/type" [& ^:type props] {:nickname "updateType" :summary "Update type"}
        (if-not (p/type-exists? (props :type))
          (conflict {:msg "Type does not exists, use POST /type first"}) 
          (do (p/update-type props) 
              (success {:msg "type updated"}))))

  (DELETE- "/type/:type" [^:string type] {:nickname "deleteType" :summary "Delete type" 
                                          :errorResponses (errors {:bad-req "Type does not exist"})}
           (if (p/type-exists? type)
             (do (p/delete-system type) 
                 (success {:msg "Type deleted"}))
             (bad-req {:msg "Type does not exist"}))) 
  )


