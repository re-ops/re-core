(ns re-core.persistency.core
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.redis :refer (server-conn)]
   [components.core :refer (Lifecyle)]
   [puny.redis :as r]))

(refer-timbre)

(defn initilize-puny
  "Initlizes puny connection"
  []
  (info "Initializing puny connection" (server-conn))
  (r/server-conn (server-conn)))

(defrecord Persistency
           []
  Lifecyle
  (setup [this]
    (initilize-puny))
  (start [this])
  (stop [this]))

(defn instance
  "creats a jobs instance"
  []
  (Persistency.))
