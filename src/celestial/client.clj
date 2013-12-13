(ns celestial.client
  "celestial clojure client"
  (:require 
    [clojure.core.strint :refer (<<)]
    [clj-http.client :as client]))

(def auth-store (atom ["user" "password"]))
(def root "https://localhost:8443/")

(defn with-auth 
  "perform verb with basic auth" 
  [verb url & args]
  (:body (verb (<< "~{root}/~{url}") 
    (merge args {:insecure? true :basic-auth @auth-store :as :json}))))

(defn system 
  "get system" 
  [id]
  (with-auth client/get "systems/1"))

(comment 
  (reset! auth-store ["admin" "changeme"])
  (system 1))
