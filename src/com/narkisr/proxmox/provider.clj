(ns com.narkisr.proxmox.provider
  (:require 
    [closchema.core :as schema])
  (:use 
    [taoensso.timbre :only (debug info error)]
    clojure.core.strint
    [com.narkisr.celestial.core]
    [com.narkisr.proxmox.remote :only (prox-post prox-delete)])
  )

(def ct-schema
  {:type "object"
   :properties  {
                 :vmid  {:description  "vm unique id number" :type  "integer" :required  true}
                 :ostemplate  {:description  "template used " :type  "string" :required  true}
                 :cpus  {:description "number of cpus" :type  "integer" :required false} 
                 :disk  {:description "disk size" :type  "integer" :required false}
                 :memory  {:description "RAM size" :type  "integer" :required false} 
                 :ip_address  {:description "the container static ip" :type  "string" :required false}
                 :password  {:description "machines password" :type  "string" :required false}
                 :hostname  {:description "container hostname" :type  "string" :required false}
                 }
   })

(defn validate [spec]
  (schema/report-errors (schema/validate ct-schema spec)))


(deftype Container [node spec]
  Vm
  (create [this] 
    (use 'com.narkisr.proxmox.remote);TODO this is ugly http://tinyurl.com/avotufx fix with macro
    (use 'com.narkisr.proxmox.provider)
    (let [errors (validate spec)]
      (if (empty? errors) 
        (try (prox-post (str "/nodes/" node "/openvz") spec)
          (catch Exception e (error e))) 
        (throw (RuntimeException. (str errors))))))
  (delete [this]
    (use 'com.narkisr.proxmox.remote)
    (try 
      (prox-delete (str "/nodes/" node "/openvz/" (:vmid spec)))
      (catch Exception e (error e))))
  (start [this]
    (use 'com.narkisr.proxmox.remote)
    (try 
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/start"))
      (catch Exception e (error e)))) 
  (stop [this]
    (use 'com.narkisr.proxmox.remote)
    (try 
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/stop"))
      (catch Exception e (error e))))
  ) 




;
