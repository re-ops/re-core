(ns lxc.client
  "LXC http client see https://github.com/lxc/lxd/blob/master/doc/rest-api.md

   Using p12 file format cert that can be created from lxc key and crt files:

     openssl pkcs12 -export -out certificate.p12 -inkey client.key -in client.crt -certfile servercerts/127.0.0.1.crt
  "
  (:require
   [re-share.core :refer (wait-for)]
   [clojure.string :as s :refer (lower-case join)]
   [clojure.data.json :as json]
   [less.awful.ssl :refer (ssl-context->engine ssl-p12-context)]
   [clojure.core.strint :refer (<<)]
   [org.httpkit.client :as http])
  (:import clojure.lang.ExceptionInfo))

(defn context [{:keys [path p12 password crt]}]
  (ssl-p12-context (<< "~{path}/~{p12}") (char-array password) (<< "~{path}/servercerts/~{crt}")))

(defn ssl-opts [node]
  {:sslengine (ssl-context->engine (context node))})

(defn into-body [m]
  {:body (json/write-str m) :headers {"Content-Type" "application/json"}})

(defn parse
  [response]
  (let [{:keys [status body error] :as result} (deref response)]
    (when error
      (throw error))
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

(defonce max-async-attempts 30)

(defn async-status
  "Poll for async container operations return final status"
  [node {:keys [operation] :as m}]
  (loop [metadata {:status "running"} attempts 0]
    (when (> attempts max-async-attempts)
      (throw (ex-info (<< "Failed to poll for successful async operation state after ~{attempts} attempts") m)))
    (Thread/sleep 1000)
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
  {:pre [name]}
  (run http/get (<< "containers/~{name}") node))

(defn state
  "Get container current state"
  [node {:keys [name]}]
  {:pre [name]}
  (run http/get (<< "containers/~{name}/state") node))

(defn delete
  "Get container information"
  [node {:keys [name]}]
  {:pre [name]}
  (async-status node (run http/delete (<< "containers/~{name}") node)))

(defn into-names [{:keys [metadata]}]
  (map #(s/replace % #"\/1.0\/containers\/" "") metadata))

(defn list
  "List containers in lxd instance"
  [node]
  (run http/get "containers" node))

(defn image
  "Get image information"
  [node fingerprint]
  (run http/get (<< "images/~{fingerprint}") node))

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
  {:pre [name]}
  (change-state node name "start"))

(defn stop
  "stop container"
  [node {:keys [name]}]
  {:pre [name]}
  (change-state node name "stop"))

(defn ip [node container]
  (:address
   (first
    (filter (fn [{:keys [family]}] (= family "inet"))
            (get-in (state node container) [:metadata :network :eth0 :addresses])))))

(defn wait-for-ip [node container timeout]
  (wait-for {:timeout timeout :sleep [2000 :ms]}
            #(not (nil? (ip node container)))
            "Timed out on waiting for container ip "))

(defn status [node container]
  (try (-> (state node container)
           (get-in [:metadata :status])
           lower-case
           keyword)
       (catch ExceptionInfo e
         (when-not (= 404 (:status (ex-data e)))
           (throw e)))))

(comment
  (require '[re-core.model :refer (hypervisor)])

  (def node
    (merge {:host "127.0.0.1" :port "8443"} (hypervisor :lxc :auth)))

  (def m  {:name "test-1"
           :architecture "x86_64"
           :profiles ["default"]
           :description "{foo:1}"
           :devices {}
           :ephemeral false
           :config {:limits.cpu "1" :limits.memory "500"}
           :source {:type "image" :alias "ubuntu-18.04.2_node-8.x"}})

  (require '[clojure.pprint :refer (pprint)])

  (re-core.common/print-e (create node m))

  (list node)

  (delete node m)

  (ip node m)

  (pprint (get node m))

  (pprint (image node "6bdd4ab498605ce6cc8a44220f4664581b1201c448e9c4e1451bc90c2e31084c"))

  (pprint (state node {:name "basic-2721567e8c"}))

  (into-names (list node)))
