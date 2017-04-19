(ns user
  (:use midje.repl)
  (:require
     [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars)]
     [clojure.java.io :as io]
     [clojure.pprint :refer (pprint)]
     [clojure.repl :refer :all]
     [clojure.tools.namespace.repl :refer (refresh refresh-all)]
     [re-mote.repl :refer :all]
     [re-core.repl :refer :all]
     [re-core.repl.fixtures :refer :all]
     [re-core.launch :as core]
     [re-mote.launch :as mote]))

(def system nil)

(re-core.model/set-dev)

(defn setup-all []
  (mote/setup)
  (core/setup))

(defn start-all [s]
  (mote/start nil)
  (core/start s))

(defn stop-all [s]
  (mote/stop nil)
  (core/stop s))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (setup-all))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system start-all))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system (fn [s] (when s (stop-all s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

