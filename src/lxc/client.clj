(ns lxc.client
  "Lxc http client see https://github.com/lxc/lxd/blob/master/doc/rest-api.md

   Using p12 file format cert that can be created from lxc key and crt files:

     openssl pkcs12 -export -out certificate.p12 -inkey client.key -in client.crt -certfile servercerts/127.0.0.1.cr
  "
  (:require
   [clojure.string :refer (lower-case join)]
   [clojure.data.json :as json]
   [re-core.model :refer (hypervisor)]
   [less.awful.ssl :refer (ssl-context->engine ssl-p12-context)]
   [clojure.core.strint :refer (<<)]
   [org.httpkit.client :as http]))

(defn context [{:keys [path p12 password crt]}]
  (ssl-p12-context (<< "~{path}/~{p12}") (char-array password) (<< "~{path}/servercerts/~{crt}")))

(defn ssl-opts [node]
  {:sslengine (ssl-context->engine (context node))})

(defn into-body [m]
  {:body (json/write-str m) :headers {"Content-Type" "application/json"}})

(defn parse [response]
  (let [{:keys [status body] :as result} (deref response)]
    (if (and (>= status 400) (<= status 599))
      (throw (ex-info (<< "Failed to process lxc api request got ~{status} code") result))
      (json/read-str body :key-fn keyword))))

(defn run
  ([verb endpoint node]
   (run verb endpoint node {}))
  ([verb endpoint {:keys [host port] :as node} opts]
   (parse (verb (<< "https://~{host}:~{port}/1.0/~{endpoint}") (merge (ssl-opts node) opts)))))

(defn into-status [s]
  (keyword (lower-case s)))

(defn async-status
  "Poll for async container operations return final status"
  [node {:keys [operation status] :as m}]
  (loop [metadata {:status "running"} attempts 0]
    (when (> attempts 30)
      (throw (ex-info "Failed to pull for the async operation status" m)))
    (Thread/sleep 100)
    (let [status-key (into-status (metadata :status))]
      (if-not (= :running status-key)
        (if-not (= :success status-key)
          (throw (ex-info (<< "Async operation failed due to '~(metadata :err)'") metadata))
          :success)
        (recur
         (:metadata (run http/get (subs operation 4) node)) (+ 1 attempts))))))

(defn get
  "Get container information"
  [node {:keys [name]}]
  (run http/get (<< "containers/~{name}") node))

(defn state
  "Get container current state"
  [node {:keys [name]}]
  (run http/get (<< "containers/~{name}/state") node))

(defn delete
  "Get container information"
  [node {:keys [name]}]
  (async-status node (run http/delete (<< "containers/~{name}") node)))

(defn list
  "List containers in lxd instance"
  [node]
  (run http/get "containers" node))

(defn create
  "Create container using http api"
  [node container]
  (async-status node (run http/post "containers" node (into-body container))))

(defn action [op]
  {:action op :timeout 5000 :force false :stateful false})

(defn change-state
  "Change container state"
  [node name op]
  (async-status node (run http/put (<< "containers/~{name}/state") node (into-body (action op)))))

(defn start
  "start container"
  [node {:keys [name]}]
  (change-state node name "start"))

(defn stop
  "stop container"
  [node {:keys [name]}]
  (change-state node name "stop"))

(defn ip [node container]
  (:address
   (first
    (filter (fn [{:keys [family]}] (= family "inet"))
            (get-in (state node container) [:metadata :network :eth0 :addresses])))))

(defn status [node container]
  (-> (state node container)
      (get-in [:metadata :status])
      lower-case
      keyword))

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

  (require '[clojure.pprint :refer (pprint)])

  (start node m)

  (stop node m)

  (status node m)

  (pprint (state node m))

  (list node))