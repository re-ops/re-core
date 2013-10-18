(ns celestial.fixtures.core
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.redis :as r]
    [celestial.persistency.systems :as s]
    ))

(defn is-type? [type]
  (fn [exception] 
    (= type (get-in (.getData exception) [:object :type]))))

(defn with-m? [m]
  (fn [actual]
    (= (get-in (.getData actual) [:object :errors]) m)))


(defmacro with-admin [& body]
  `(with-redefs [celestial.security/current-user (fn [] {:username "admin"})
                celestial.persistency/get-user! (fn [a#] celestial.fixtures.data/admin)]
       ~@body 
       ))

(defmacro with-conf 
  "Using fixture/celestial.edn conf file"
  [f & body]
  (if (symbol? f)
    `(with-redefs [celestial.config/config ~f celestial.model/env :dev]
       ~@body 
       )
    `(with-redefs [celestial.config/config celestial.fixtures.data/local-prox celestial.model/env :dev ]
       ~@(conj body f) 
       )))

(defmacro with-defaults
  "A fact that includes default conf and admin user" 
  [& args]
  `(with-admin
    (with-conf ~@args)))

;; (clojure.pprint/pprint (macroexpand '(with-conf local-conf)))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))


