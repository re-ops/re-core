(ns lxc.client
  "lxc http client"
  (:require
   [clojure.data.json :as json]
   [re-share.config :refer (get!)]
   [re-core.model :refer (hypervisor)]
   [less.awful.ssl :refer (ssl-context->engine ssl-p12-context)]
   [clojure.core.strint :refer (<<)]
   [org.httpkit.client :as http]))


; openssl pkcs12 -export -out certificate.p12 -inkey client.key -in client.crt -certfile servercerts/127.0.0.1.cr


(defn context [{:keys [path p12 password crt]}]
  (ssl-p12-context (<< "~{path}/~{p12}") (char-array password) (<< "~{path}/servercerts/~{crt}")))

(defn ssl-opts [node]
  {:sslengine (ssl-context->engine (context node))})

(defn into-body [m]
  {:body (json/write-str m) :headers {"Content-Type" "application/json"}})

(defn parse [response]
  (let [{:keys [status body] :as result} (deref response)]
    (if (and (>= status 400) (<= status 599))
      (throw (ex-info "failed to process request" result))
      (json/read-str body :key-fn keyword))))

(defn run
  ([verb endpoint node]
   (run verb endpoint node {}))
  ([verb endpoint {:keys [host port] :as node} opts]
   (parse (verb (<< "https://~{host}:~{port}/1.0/~{endpoint}") (merge (ssl-opts node) opts)))))

(defn get
  "Get container information"
  [node name]
  (run http/get (<< "containers/~{name}") node))

(defn delete
  "Get container information"
  [node name]
  (run http/delete (<< "containers/~{name}") node))

(defn list
  "List containers in lxd instance"
  [node]
  (run http/get "containers" node))

(defn create
  "Create container using http api"
  [node container]
  (run http/post "containers" node (into-body container)))

(comment
  (def node
    (merge {:host "127.0.0.1" :port "8443"} (hypervisor :lxc :auth)))

  (def m  {:name "my-new-container"
           :architecture "x86_64"
           :profiles ["default"]
           :devices {}
           :ephemeral false
           :config {:limits.cpu "2"}
           :source {:type "image" :alias "ubuntu-18.04"}})

  (list node)
  (get node "my-new-container")
  (delete node "my-new-container")
  (create node m))
