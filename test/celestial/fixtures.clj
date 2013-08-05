(ns celestial.fixtures
  (:import 
     java.awt.datatransfer.StringSelection
     java.awt.Toolkit)
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.redis :as r]
    [celestial.persistency :as p])
  (:use [celestial.common :only (slurp-edn)]))

(def redis-prox-spec (slurp-edn "fixtures/redis-system.edn"))

(def redis-type (slurp-edn "fixtures/redis-type.edn"))

(def redis-ec2-spec (slurp-edn "fixtures/redis-ec2-system.edn"))

(def redis-vc-spec (slurp-edn "fixtures/redis-vc-system.edn"))

(def local-prox (slurp-edn "fixtures/.celestial.edn"))

(def redis-actions (slurp-edn "fixtures/redis-actions.edn"))

(def user-quota (slurp-edn "fixtures/user-quota.edn"))

(defn clipboard-copy [s]
  (let [clp (.getSystemClipboard (Toolkit/getDefaultToolkit))]
    (.setContents clp (StringSelection. s) nil)
   )
  )

;; (clipboard-copy (clojure.data.json/write-str redis-type :escape-slash false))
;; (clipboard-copy (clojure.data.json/write-str redis-prox-spec :escape-slash false))

(defn is-type? [type]
  (fn [exception] 
    (= type (get-in (.getData exception) [:object :type]))))

(defn with-m? [m]
  (fn [actual]
    (= (get-in (.getData actual) [:object :errors]) m)))

(defmacro with-conf 
  "Using fixture/.celestial.edn conf file"
  [& body]
 `(with-redefs [celestial.config/config celestial.fixtures/local-prox]
   ~@body
    ))

(defn populate []
  (r/clear-all)
  (p/add-type redis-type)
  (p/add-action redis-actions)
  (doseq [i (range 100)] 
    (if (= 0 (mod i 2)) 
      (p/add-system redis-prox-spec)
      (p/add-system redis-ec2-spec))))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(def puppet-ami (merge-with merge redis-ec2-spec {:aws {:image-id "ami-f5e2ff81" :key-name host}}))
