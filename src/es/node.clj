(ns es.node
  "Elasticsearch connection"
  (:require
   [clojure.core.strint :refer (<<)]
   [qbits.spandex :as s]
   [taoensso.timbre :refer (refer-timbre)]
   [safely.core :refer [safely]]
   [re-core.common :refer (get!)]))

(refer-timbre)

(def c (atom nil))
(def snif (atom nil))

(defn connect-
  "Connecting to Elasticsearch"
  []
  (let [{:keys [host port]} (get! :elasticsearch)]
    (when-not @c
      (info "Connecting to elasticsearch")
      (reset! c
              (s/client {:hosts [(<< "http://~{host}:~{port}")]
                         :basic-auth {:user "elastic" :password "changeme"}}))
      (reset! snif (s/sniffer @c)))))

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
  "Reset connection atom"
  []
  (info "Reset elasticsearch connection atom")
  (s/close! @c)
  (s/close! @snif)
  (reset! snif nil)
  (reset! c nil))
