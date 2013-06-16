(ns hooks.hubot
  "Hubot notification post hook"
  (:use 
    [clojure.core.strint :only (<<)])
  (:require 
    [clj-http.client :as client]))

(defn reply [workflow event]
  (get-in 
    {:reload {:success "Managed to create system" :error "Failed to create system" }
     :destroy {:success "Destroyed system" :error "Failed to destroy system"} 
     :puppetize {:success "Provisioned system" :error "Failed to provision system"}
     :run-action {:success "Manage to run action on system " :error "Failed to run action on system"}}
     [workflow event]))

(defn notify-hubot 
  "notify a hubot instance that a machine is up" 
  [{:keys [event workflow system-id hubot-host]}]
  {:pre [hubot-host]}
  (client/post (<< "~{hubot-host}/hubot/creation-notify") 
               {:body (<< "{\"id\": \"~{system-id}\", \"msg\": \"~(reply workflow event)\"}")
                :content-type :json
                :socket-timeout 1000 :conn-timeout 1000 :accept :json})) 


;; (notify-hubot {:system-id "1"  :hubot-host "http://192.168.5.14:8083" :msg "hello"})

