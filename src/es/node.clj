(ns es.node
  "ES connection"
  (:require
   [qbits.spandex :as s]
   [taoensso.timbre :refer (refer-timbre)]
   [safely.core :refer [safely]]
   [re-core.common :refer (get!)]
   ))

(refer-timbre)

(def c (atom nil))

(defn connect-
  "Connecting to Elasticsearch"
  []
  (let [{:keys [host port]} (get! :elasticsearch)]
    (info "Connecting to elasticsearch")
    (reset! ES 
      (s/client {
          :hosts [(<< "http://~{host}:~{port}")] 
          :basic-auth {:user "elastic" :password "changeme"}}))))

(defn connect
  "Connecting to Elasticsearch with retry support"
  []
  (let [{:keys [host port cluster]} (get! :elasticsearch)]
    (safely (connect-)
            :on-error
            :max-retry 5
            :message "Error while trying to connect to Elasticsearch"
            :log-errors true
            :retry-delay [:random-range :min 2000 :max 5000])))

(defn stop
  "stops embedded ES node"
  []
  (info "Stoping local elasticsearch node")
  (reset! ES nil))
