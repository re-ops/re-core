(ns com.narkisr.proxmox.remote
  (:require 
    [taoensso.timbre :only (debug info error)]
    [cheshire.core :refer :all]
    [clj-http.client :as client]
    [clj-config.core :as conf])
  (:use clojure.core.strint))

(def config (conf/read-config (<<  "~(System/getProperty \"user.home\")/.multistage.edn")))

(def root (<< "https://~(-> config :hypervisor :host):8006/api2/json"))

(defn retry
 "A retry for http calls against proxmox"
  [ex try-count http-context]
 
  (println "Got:" ex)
  (if (> try-count 4) false true))

(defn root-post [api args]
  (client/post (str root api) (assoc args :insecure? true)))

(defn root-delete [api args]
  (client/delete (str root api) (assoc args :insecure? true)))

(defn login []
  (let [res (root-post "/access/ticket" {:form-params (dissoc (assoc (config :hypervisor) :realm "pam") :host)})]
    (select-keys  
      (:data (parse-string (:body res) true))   
      [:CSRFPreventionToken :ticket])))

(def auth-headers
  (let [{:keys [CSRFPreventionToken ticket] :as headers} (login)]
    (assoc 
      (assoc {} "Cookie" (str "PVEAuthCookie=" ticket)) "CSRFPreventionToken" CSRFPreventionToken)))

(defn prox-post 
  "Do a post against a proxmox instance with provided params with a retry count"
  ([api] (prox-post api retry nil))
  ([api params] 
   (if (nil? params)
     (root-post api {:headers auth-headers}) 
     (root-post api {:form-params params :headers auth-headers}))))

(defn prox-delete [api]
  (root-delete api {:headers auth-headers}))
