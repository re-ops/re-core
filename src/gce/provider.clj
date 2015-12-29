(ns gce.provider
  (:require
   [clojure.core.strint :refer (<<)]
   [clojure.data.json :refer (write-str)]
   [gce.validations :refer (validate-provider)]
   [celestial.provider :refer (wait-for-ssh mappings wait-for transform os->template)]
   [clojure.java.data :refer (to-java from-java)]
   [celestial.core :refer (Vm)] 
   [taoensso.timbre :as timbre]
   [celestial.persistency.systems :as s]
   [celestial.model :refer (translate vconstruct hypervisor)])
  (:import 
    (com.google.api.services.compute.model Instance)
    (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
    (com.google.api.client.json.jackson2 JacksonFactory)
    (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
    (com.google.api.services.compute Compute$Builder ComputeScopes)
    )
 )

(timbre/refer-timbre)

(defn build-compute [service-file]
  (let [auth  (GoogleCredential/fromStream (clojure.java.io/input-stream service-file))
        transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (JacksonFactory/getDefaultInstance)
        scopes [ComputeScopes/COMPUTE]]
       (-> (Compute$Builder. transport json-factory nil)
         (.setApplicationName "celestial")
         (.setHttpRequestInitializer (.createScoped auth scopes))
         (.build))))

(defn instances [compute]
  (-> compute (.instances)))

(defn list-instances [compute project zone]
  (let [items (get (-> (instances compute) (.list project zone) (.execute)) "items")]
    (map clojure.walk/keywordize-keys items)))

(defn create-instance [compute gce {:keys [project-id zone] :as spec}] 
  (let [instance (.fromString (JacksonFactory/getDefaultInstance) (write-str gce) Instance)]
    (-> (instances compute) (.insert project-id zone instance)) 
    ))
 
(defmacro with-id [& body])

(defrecord GCEInstance [compute gce spec]
  Vm
  (create [this] 
    (let [{:keys [id] :as instance} (create-instance gce) ]
     (debug "created" id)
     (s/partial-system (spec :system-id) {:gce {:id id}})
      this))

  (start [this]
    (with-id
      (let [{:keys [machine]} (s/get-system (spec :system-id))]
        (when-not (= "running" (.status this))
          )
        (wait-for-ssh (machine :ip) "root" [5 :minute]))
      ))

  (delete [this])

  (stop [this])

  (status [this]))

(defn into-gce [{:keys [name machine-type zone image]}]
  {
    :name name
    :machineType (<< "zones/~{zone}/machineTypes/~{machine-type}")
    :disks [{
      :initializeParams {:sourceImage image} :autoDelete true
      :type "PERSISTENT" :boot true
    }]
    :networkInterfaces [{
      :accessConfigs [{:type "ONE_TO_ONE_NAT", :name "External NAT" }]
      :network "global/networks/default"
    }]
 })

(defn into-system [system-id])

(def machine-ts 
  "Construcuting machine transformations"
   {:image (fn [os] (:image ((os->template :gce) os)))})

(defmethod translate :gce [{:keys [machine gce system-id] :as spec}]
    "Convert the general model into a gce instance"
    (-> (merge machine gce {:system-id system-id})
      (mappings {:os :image :hostname :name})
      (transform machine-ts)
      ((juxt into-gce (partial select-keys [:system-id :project-id])))
    ))

(defn validate [spec &] 
  (validate-provider spec) spec)

(defmethod vconstruct :gce [spec]
  (let [compute (build-compute (hypervisor :gce :service-file))]
   (apply (partial ->GCEInstance compute) (validate (translate spec)))))

