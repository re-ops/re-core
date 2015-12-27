(ns gce.provider
  (:require
   [clojure.data.json :refer  (write-str)]
   [celestial.persistency.systems :as s]
   [celestial.model :refer (translate vconstruct)])
  (:import 
    (com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow$Builder)
    (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
    (com.google.api.client.json.jackson2 JacksonFactory)
    (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
    (com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets)
    (com.google.api.services.compute Compute$Builder ComputeScopes)
    (java.io InputStreamReader ByteArrayInputStream)
    (com.google.api.client.util.store DataStoreFactory FileDataStoreFactory)
    (com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp)
    (com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver)
    )
 )


(defn build-compute [service-file]
  (let [auth  (GoogleCredential/fromStream (clojure.java.io/input-stream service-file))
        transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (JacksonFactory/getDefaultInstance)
        scopes [ComputeScopes/COMPUTE]]
       (-> (Compute$Builder. transport json-factory nil)
         (.setApplicationName "celestial")
         (.setHttpRequestInitializer (.createScoped auth scopes))
         (.build))))

(defn list-vms [compute project zone]
  (let [items (get (-> compute (.instances) (.list project zone ) (.execute)) "items")]
    (map clojure.walk/keywordize-keys items)))



(defmethod vconstruct :gce [spec])
