(ns lxc.client
  "lxc http client"
  (:require
   [less.awful.ssl :refer (ssl-context->engine ssl-p12-context)]
   [clojure.core.strint :refer (<<)]
   [org.httpkit.client :as http]))


; openssl pkcs12 -export -out certificate.p12 -inkey client.key -in client.crt -certfile servercerts/127.0.0.1.cr


(def context
  (less.awful.ssl/ssl-p12-context
   "certificate.p12"
   (char-array "")
   "127.0.0.1.crt"))

(def opts
  {:sslengine (ssl-context->engine context)})

(defn list
  "list containers in lxd instance"
  [{:keys [host port] :as node}]
  (let [{:keys [status body error]} @(http/get (<< "https://~{host}:~{port}/1.0/containers") opts)]
    (if error
      error
      body)))

(defn create
  "Create container using http api"
  [m]
  ())

(comment

  (list {:host "127.0.0.1" :port "8443"})

  (def m  {:name "my-new-container"
           :architecture "x86_64"
           :profiles ["default"]
           :ephemeral false
           :config {:limits.cpu "2"}
           :source {:type "image" :alias "ubuntu:18:04"}}))
