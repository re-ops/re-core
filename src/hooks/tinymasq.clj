(ns hooks.tinymasq
  "Tinymasq registration hook for static addresses using hosts file: "
  (:require
   [slingshot.slingshot :refer  [throw+ try+]]
   [clojure.data.json :refer (write-str)]
   [clj-http.client :as client]
   [re-core.persistency.systems :as s]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn into-json  [domain machine]
  (write-str {:hostname (str (machine :hostname) "." domain) :ip (machine :ip)}))

(defn call
  "calls remote tinymasq"
  [verb machine {:keys [user password domain tinymasq] :as args}]
  (debug verb (dissoc args :password))
  (try+
   (:body
    (verb (<< "~{tinymasq}/hosts")
          {:body (into-json domain machine) :basic-auth  [user password]
           :content-type :json :socket-timeout 1000
           :conn-timeout 1000 :accept :json :insecure? true}))
   (catch [:status 401] e
     (error "Auth fail check tinymasq user/password") (throw+ e))))

(defn update-host
  [{:keys [system-id] :as args}]
  (call client/put (:machine (s/get-system system-id)) args))

(defn add-host
  [{:keys [system-id] :as args}]
  (call client/post (:machine (s/get-system system-id)) args))

(defn remove-host
  [{:keys [machine] :as args}]
  (call client/delete machine args))

(def actions {:reload {:success update-host} :create {:success add-host}
              :start {:success add-host} :stop {:success remove-host}
              :destroy {:success remove-host :error remove-host}
              :stage {:success add-host}})

(defn update-dns [{:keys [event workflow] :as args}]
  ((get-in actions [workflow event] (fn [_] nil)) args))

