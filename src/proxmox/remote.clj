(ns proxmox.remote
  (:require 
    [cheshire.core :refer :all]
    [clj-http.client :as client]
    [clj-config.core :as conf])
  (:use clojure.core.strint
        [slingshot.slingshot :only  [throw+ try+]]
        [taoensso.timbre :only (debug info error) :as timbre])
  (:import clojure.lang.ExceptionInfo)) 

;(timbre/set-level! :debug)

(def config (conf/read-config (<<  "~(System/getProperty \"user.home\")/.multistage.edn")))

(def root (<< "https://~(-> config :hypervisor :host):8006/api2/json"))

(defn retry
  "A retry for http calls against proxmox, some operations lock machine even after completion"
  [ex try-count http-context]
  (debug "re-trying due to" ex "attempt" try-count)
  (Thread/sleep 1000) 
  (if (> try-count 1) false true))

(def http-opts 
  {:insecure? true :retry-handler retry})

(declare auth-headers)

(defn call [verb api args]
  (:data (parse-string 
    (:body (verb (str root api) (merge args http-opts {:headers (auth-headers)}))) true)))

(defn call- [verb api args]
  "Calling without auth headers"
  (:data (parse-string (:body (verb (str root api) (merge args http-opts))) true)))

(def login-creds (dissoc (assoc (config :hypervisor) :realm "pam") :host))

(defn login []
  {:post [(not (nil? (% :CSRFPreventionToken))) (not (nil? (% :ticket)))]}
  (try+
    (let [res (call- client/post "/access/ticket" {:form-params login-creds})]
      (select-keys res [:CSRFPreventionToken :ticket]))
    (catch #(#{401 500} (:status %)) e
      (throw (ExceptionInfo. "Failed to login" config)))))

(def auth-headers
  (memoize 
    (fn []
      (let [{:keys [CSRFPreventionToken ticket] :as headers} (login)]
        (assoc 
          (assoc {} "Cookie" (str "PVEAuthCookie=" ticket)) "CSRFPreventionToken" CSRFPreventionToken)))))

(defn prox-post 
  "A post against a proxmox instance with provided params"
  ([api] (prox-post api nil))
  ([api params] 
   (if (nil? params)
     (call client/post api {:headers auth-headers}) 
     (call client/post api {:form-params params :headers auth-headers}))))

(defn prox-delete [api] (call client/delete api {:headers auth-headers}))

(defn prox-get [api] (call client/get api {:headers auth-headers}))


