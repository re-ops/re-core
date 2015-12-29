(ns gce.provider
  (:require
   [clojure.core.strint :refer (<<)]
   [clojure.data.json :refer (write-str)]
   [gce.validations :refer (validate-provider)]
   [celestial.provider :refer (wait-for-ssh mappings wait-for transform os->template)]
   [clojure.java.data :refer (to-java from-java)]
   [celestial.core :refer (Vm)] 
   [clojure.walk :refer (keywordize-keys)]
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
    (-> (instances compute) 
      (.insert project-id zone instance) (.execute) from-java keywordize-keys) 
    ))

(defn delete-instance [compute {:keys [name]} {:keys [project-id zone]}]
   (-> (instances compute) (.delete project-id zone name) (.execute)))

(defn get-instance [compute {:keys [name]} {:keys [project-id zone]}]
   (-> (instances compute) (.get project-id zone name) (.execute) from-java keywordize-keys))

(defn get-operation [compute {:keys [project-id zone]} operation]
  (-> (.zoneOperations compute) 
    (.get project-id zone (:name operation)) (.execute) from-java keywordize-keys))

(defmacro with-id [& body])

(defrecord GCEInstance [compute gce spec]
  Vm
  (create [this] 
    (let [operation (create-instance compute gce spec)]
     (wait-for {:timeout [5 :minute]} 
        #(= (:status (get-operation compute spec operation)) "DONE") 
         {:type ::gce:creation-fail} "Timed out on waiting for creation to be done") 
     (let [{:keys [natIp]} (get-instance compute gce spec)]
       (s/partial-system (spec :system-id) {:machine {:ip natIp}}))
      this))

  (start [this]
    (with-id
      (let [{:keys [machine]} (s/get-system (spec :system-id))]
        (when-not (= "running" (.status this))
          )
        (wait-for-ssh (machine :ip) "root" [5 :minute]))
      ))

  (delete [this]
     (let [{:keys [id] :as operation} (create-instance compute gce spec) ]
      (wait-for {:timeout [5 :minute]} #(= ("status" operation) "DONE") 
       {:type ::gce:fail} "Timed out on waiting for operation to be done"))
    )

  (stop [this])

  (status [this]
    (try 
      (let [{:keys [status]} (get-instance compute gce spec)]
        (.toLowerCase status))
      (catch Exception e false) 
      )))

(defn into-gce [{:keys [name machine-type tags zone image]}]
  {
    :name name
    :machineType (<< "zones/~{zone}/machineTypes/~{machine-type}")
    :tags {:items (clojure.tools.trace/trace tags)}
    :disks [{
      :initializeParams {:sourceImage image} :autoDelete true
      :type "PERSISTENT" :boot true
    }]
    :networkInterfaces [{
      :accessConfigs [{:type "ONE_TO_ONE_NAT", :name "External NAT" }]
      :network "global/networks/default"
    }]
 })

(def machine-ts 
  "Construcuting machine transformations"
   {:image (fn [os] (:image ((os->template :gce) os)))})

(defmethod translate :gce [{:keys [machine gce system-id] :as spec}]
    "Convert the general model into a gce instance"
    (-> (merge machine gce {:system-id system-id})
      (mappings {:os :image :hostname :name})
      (transform machine-ts)
      ((juxt into-gce (fn [m] (select-keys m [:system-id :project-id :zone]))))
    ))

(defn validate [[gce spec]] 
  (validate-provider gce spec) [gce spec])

(defmethod vconstruct :gce [spec]
  (let [compute (build-compute (hypervisor :gce :service-file))]
   (apply (partial ->GCEInstance compute) (validate (translate spec)))))

