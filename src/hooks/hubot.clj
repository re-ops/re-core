(ns celestial.hubot
  "Hubot notification post hook"
  (:use 
    [clojure.core.strint :only (<<)])
  (:require 
    [clj-http.client :as client]))


(defn notify-hubot 
  "notify a hubot instance that a machine is up" 
  [{:keys [id hubot-host msg]}]
  (client/post (<< "~{hubot-host}/hubot/creation-notify") 
       {:body (<< "{\"id\": \"~{id}\", \"msg\": \"~{msg}\"}") :content-type :json
         :socket-timeout 1000 :conn-timeout 1000 :accept :json})) 


