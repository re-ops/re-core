(ns user
  (:use midje.repl)
  (:require 
     [celestial.persistency [users :as u] [types :as t]]  
     [celestial.redis :refer (clear-all)]  
     [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars)]
     [celestial.persistency.systems :as s]
     [clojure.java.io :as io]
     [celestial.common :refer (slurp-edn)]
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

(defn populate 
  "basic population for dev env" 
  []
  (clear-all)
  (u/add-user 
      {:envs [:dev] :roles #{:celestial.roles/user} :username "ronen" :password "bar"})
  (u/reset-admin)
  (with-redefs [celestial.security/current-user (fn [] {:username "admin"})]
    (t/add-type (slurp-edn "fixtures/redis-type.edn"))
    (s/add-system (slurp-edn "fixtures/redis-system.edn"))))

(defn reset []
  (stop)
  #_(refresh :after 'user/go))

