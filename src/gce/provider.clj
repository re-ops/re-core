(ns gce.provider
  (:require
   [gce.validations :refer (validate-provider)]
   [celestial.provider :refer (wait-for-ssh mappings wait-for)]
   [celestial.core :refer (Vm)] 
   [taoensso.timbre :as timbre]
   [celestial.persistency.systems :as s]
   [celestial.model :refer (translate vconstruct)])
  (:import 
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

(defn list-instances [compute project zone]
  (let [items (get (-> compute (.instances) (.list project zone) (.execute)) "items")]
    (map clojure.walk/keywordize-keys items)))

(defn create-instane [spec] {})
 
(defmacro with-id [& body]
  )

(defrecord Instance [spec]
  Vm
  (create [this] 
    (let [{:keys [id] :as instance} (create-instane spec) ]
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

(defmethod translate :gce [{:keys [machine gce system-id] :as spec}]
    "Convert the general model into a gce instance"
    (mappings (merge machine gce {:system-id system-id}) []))

(defn validate [spec] 
  (validate-provider spec) spec)

(defmethod vconstruct :gce [spec]
  (Instance. (validate (translate spec))))
