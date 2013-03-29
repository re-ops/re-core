(ns celestial.logging
  "timbre appenders"
 (:require [taoensso.timbre :as timbre] )
  (:use 
     [gelfino.client :only (connect send-> client-socket) ]))



(def gelf-appender
 {:doc       "A gelf based appender"
  :min-level :debug
  :enabled?  true
  :async?    false
  :max-message-per-msecs nil 
  :fn 
    (fn [{:keys [ap-config level prefix message more] :as args}]
      (when-not @client-socket (connect))
      (send-> (get-in ap-config [:gelf :host])
         {:short_message (.substring message 0 2) :message message}) 
        )})

(timbre/set-config! [:appenders :gelf] gelf-appender)
(timbre/set-config! [:shared-appender-config :gelf]
                    {:host "localhost"})

(send->  "192.168.5.9"
         {:short_message "foo" :message "hey there"})

(timbre/debug "foo")
