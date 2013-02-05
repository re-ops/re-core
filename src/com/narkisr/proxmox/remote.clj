(ns com.narkisr.proxmox.remote
  (:require 
    [cheshire.core :refer :all]
    [clj-http.client :as client]
    [clj-config.core :as conf])
  (:use clojure.core.strint
        [taoensso.timbre :only (debug info error)])
  (:import clojure.lang.ExceptionInfo)) 

(def config (conf/read-config (<<  "~(System/getProperty \"user.home\")/.multistage.edn")))

(def root (<< "https://~(-> config :hypervisor :host):8006/api2/json"))

(defn retry
  "A retry for http calls against proxmox, 
  this is useful since some operations lock machine after completion"
  [ex try-count http-context]
  (debug "re-trying due to " ex " attempt " try-count)
  (.sleep (Thread/currentThread) 500)
  (if (> try-count 2) false true))

(defn root-post [api args]
  (client/post (str root api) (assoc args :insecure? true :retry-handler retry)))

(defn root-delete [api args]
  (client/delete (str root api) (assoc args :insecure? true :retry-handler retry)))

(def login-creds (dissoc (assoc (config :hypervisor) :realm "pam") :host))

(defn login []
  (try
    (let [res (root-post "/access/ticket" {:form-params login-creds})]
      (select-keys (:data (parse-string (:body res) true)) [:CSRFPreventionToken :ticket]))
    (catch Exception e 
      (throw (ExceptionInfo. "Failed to login" config)))))

(def auth-headers
  (let [{:keys [CSRFPreventionToken ticket] :as headers} (login)]
    (assoc 
      (assoc {} "Cookie" (str "PVEAuthCookie=" ticket)) "CSRFPreventionToken" CSRFPreventionToken)))

(defn prox-post 
  "A post against a proxmox instance with provided params"
  ([api] (prox-post api nil))
  ([api params] 
   (if (nil? params)
     (root-post api {:headers auth-headers}) 
     (root-post api {:form-params params :headers auth-headers}))))

(defn prox-delete [api]
  (root-delete api {:headers auth-headers}))
