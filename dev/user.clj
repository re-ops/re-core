(ns user
  (:use midje.repl)
  (:require 
     [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars) :as t]
     [clojure.java.io :as io]
     [clojure.string :as str]
     [clojure.pprint :refer (pprint)]
     [clojure.repl :refer :all]
     [clojure.tools.namespace.repl :refer (refresh refresh-all)]
     [celestial.launch :as launch]))

(def system nil)

(celestial.model/set-dev)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (launch/setup))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system launch/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system (fn [s] (when s (launch/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
