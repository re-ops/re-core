(ns gce.provider
  (:require
   [clojure.core.strint :refer (<<)]
   [clojure.data.json :refer (write-str)]
   [gce.validations :refer (validate-provider)]
   [re-core.provider :refer (wait-for-ssh mappings wait-for transform os->template)]
   [clojure.java.data :refer (to-java from-java)]
   [re-core.core :refer (Vm)]
   [clojure.walk :refer (keywordize-keys)]
   [taoensso.timbre :as timbre]
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.persistency.systems :as s]
   [re-core.model :refer (translate vconstruct hypervisor clone)])
  (:import
   (com.google.api.services.compute.model Instance)
   (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
   (com.google.api.client.json.jackson2 JacksonFactory)
   (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
   (com.google.api.services.compute Compute$Builder ComputeScopes)))

(timbre/refer-timbre)

(defn build-compute [service-file]
  (let [auth  (GoogleCredential/fromStream (clojure.java.io/input-stream service-file))
        transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (JacksonFactory/getDefaultInstance)
        scopes [ComputeScopes/COMPUTE]]
    (-> (Compute$Builder. transport json-factory nil)
        (.setApplicationName "re-core")
        (.setHttpRequestInitializer (.createScoped auth scopes))
        (.build))))

(defn instances [compute] (-> compute (.instances)))

(defn list-instances [compute project zone]
  (let [items (get (-> (instances compute) (.list project zone) (.execute)) "items")]
    (map keywordize-keys items)))

(defn create-instance [compute gce {:keys [project-id zone] :as spec}]
  (let [instance (.fromString (JacksonFactory/getDefaultInstance) (write-str gce) Instance)]
    (-> (instances compute)
        (.insert project-id zone instance) (.execute) from-java keywordize-keys)))

(defmacro run [action]
  `(-> (instances ~'compute)
       (~action (~'spec :project-id) (~'spec :zone) (~'gce :name))
       (.execute) from-java keywordize-keys))

(defn get-operation [compute {:keys [project-id zone]} operation]
  (-> (.zoneOperations compute)
      (.get project-id zone (:name operation)) (.execute) from-java keywordize-keys))

(defmacro with-id [& body])

(defn wait-for-operation [compute gce spec operation]
  (wait-for {:timeout [5 :minute]}
            #(= (:status (get-operation compute spec operation)) "DONE")
            {:type ::gce:operation-fail} (<< "Timed out on waiting for operation ~{operation}")))

(defn ip-from [instance]
  (-> instance :networkInterfaces first (get "accessConfigs") first (get "natIP")))

(defrecord GCEInstance [compute gce spec]
  Vm
  (create [this]
    (wait-for-operation compute gce spec (create-instance compute gce spec))
    (let [instance (run .get) ip (ip-from instance)]
      (s/partial-system (spec :system-id) {:machine {:ip ip}})
      (wait-for-ssh ip (spec :user) [5 :minute]))
    this)

  (start [this]
    (with-id
      (let [{:keys [machine]} (s/get-system (spec :system-id))]
        (when-not (= "running" (.status this)))
        (let [operation]
          (wait-for-operation compute gce spec (run .start)))
        (wait-for-ssh (machine :ip) (spec :user) [5 :minute]))))

  (delete [this]
    (wait-for-operation compute gce spec (run .delete)))

  (stop [this]
    (wait-for-operation compute gce spec (run .stop)))

  (status [this]
    (try (.toLowerCase (:status (run .get)))
         (catch Exception e false)))

  (ip [this]
    (get-in (s/get-system (spec :system-id)) [:machine :ip])))

(defn add-ip
  [static-ip gce]
  (if static-ip (assoc-in gce [:networkInterfaces 0 :accessConfigs 0 :natIP] static-ip) gce))

(defn into-gce [{:keys [name machine-type tags zone image static-ip]}]
  (add-ip static-ip {:name name
                     :machineType (<< "zones/~{zone}/machineTypes/~{machine-type}")
                     :tags {:items tags}
                     :disks [{:initializeParams {:sourceImage image} :autoDelete true
                              :type "PERSISTENT" :boot true}]
                     :networkInterfaces [{:accessConfigs [{:type "ONE_TO_ONE_NAT", :name "External NAT"}]
                                          :network "global/networks/default"}]}))

(def machine-ts
  "Construcuting machine transformations"
  {:image (fn [os] (:image ((os->template :gce) os)))})

(defmethod translate :gce [{:keys [machine gce system-id] :as spec}]
  ; Convert the general model into a gce instance
  (-> (merge machine gce {:system-id system-id})
      (mappings {:os :image :hostname :name})
      (transform machine-ts)
      ((juxt into-gce (fn [m] (select-keys m [:system-id :project-id :zone :user]))))))

(defn validate [[gce spec]]
  (validate-provider gce spec) [gce spec])

(defmethod vconstruct :gce [spec]
  (let [compute (build-compute (hypervisor :gce :service-file))]
    (apply (partial ->GCEInstance compute) (validate (translate spec)))))

(defmethod clone :gce [spec clone-spec]
  ; Clones the model replace unique identifiers in the process
  (-> spec (dissoc-in* [:machine :ip])))
