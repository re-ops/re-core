(ns hooks.hubot
  "Hubot notification post hook"
  (:use 
    [clojure.core.strint :only (<<)])
  (:require 
    [clj-http.client :as client]))


(defn notify-hubot 
  "notify a hubot instance that a machine is up" 
  [{:keys [system-id hubot-host msg]}]
  {:pre [hubot-host]}
  (client/post (<< "~{hubot-host}/hubot/creation-notify") 
       {:body (<< "{\"id\": \"~{system-id}\", \"msg\": \"~{msg}\"}") :content-type :json
         :socket-timeout 1000 :conn-timeout 1000 :accept :json})) 


;; (notify-hubot {:system-id "1"  :hubot-host "http://192.168.5.14:8083" :msg "hello"})

