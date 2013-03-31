(ns celestial.logging
  "timbre appenders"
  (:require [taoensso.timbre :as timbre] )
  (:use 
    [clojure.string :only (join)] 
    [gelfino.client :only (connect send-> client-socket) ]))


(def levels {:trace 1 :debug 7 :info 6 :warn 5 :error 4 :fatal 3 :unknown 1})

(def hostname (.getHostName (java.net.InetAddress/getLocalHost)))

(def gelf-appender
  {:doc       "A gelfino based appender"
   :min-level :debug
   :enabled?  true
   :async?    false
   :max-message-per-msecs nil 
   :fn 
   (fn [{:keys [ap-config level prefix message more] :as args}]
     (when-not @client-socket (connect))
     (let [message* (if (seq more) (str message " " (join " " more)) message)]
       (send-> (get-in ap-config [:gelf :host]) 
          {:short_message message* :message message* :level (levels level) 
           :facility "gelfino" :host hostname}))
     )})
