(ns celestial.fixtures
  (:import 
     java.awt.datatransfer.StringSelection
     java.awt.Toolkit)
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.redis :as r]
    [celestial.persistency.systems :as s]
    [celestial.persistency :as p])
  (:use [celestial.common :only (slurp-edn)]))

(def redis-prox-spec (slurp-edn "fixtures/redis-system.edn"))

(def redis-bridged-prox (slurp-edn "fixtures/redis-system-bridged.edn"))

(def redis-type (slurp-edn "fixtures/redis-type.edn"))

(def redis-ec2-spec (slurp-edn "fixtures/redis-ec2-system.edn"))

(def redis-vc-spec (slurp-edn "fixtures/redis-vc-system.edn"))

(def local-prox (slurp-edn "fixtures/celestial.edn"))

(def proxmox-3 (slurp-edn "fixtures/proxmox-3.edn"))

(def clustered-prox (slurp-edn "fixtures/celestial-cluster.edn"))

(def local-conf 
  (let [path (me.raynes.fs/expand-home "~/.celestial.edn")]
    (when (me.raynes.fs/exists? path) (slurp-edn path))))

(def redis-actions (slurp-edn "fixtures/redis-actions.edn"))

(def user-quota (slurp-edn "fixtures/user-quota.edn"))

(defn clipboard-copy [s]
  (let [clp (.getSystemClipboard (Toolkit/getDefaultToolkit))]
    (.setContents clp (StringSelection. s) nil)))

;; (clipboard-copy (clojure.data.json/write-str redis-type :escape-slash false))
;; (clipboard-copy (clojure.data.json/write-str redis-vc-spec :escape-slash false))

(defn is-type? [type]
  (fn [exception] 
    (= type (get-in (.getData exception) [:object :type]))))

(defn with-m? [m]
  (fn [actual]
    (= (get-in (.getData actual) [:object :errors]) m)))

(def admin {:envs [:dev :qa :prod] :roles #{:celestial.roles/admin} :username "admin" :password "foo"})
(def ronen {:envs [:dev :qa] :roles #{:celestial.roles/user} :username "ronen" :password "bar"})

(defmacro with-admin [& body]
  `(with-redefs [celestial.security/current-user (fn [] {:username "admin"})
                celestial.persistency/get-user! (fn [a#] celestial.fixtures/admin)]
       ~@body 
       ))

(defmacro with-conf 
  "Using fixture/celestial.edn conf file"
  [f & body]
  (if (symbol? f)
    `(with-redefs [celestial.config/config ~f celestial.model/env :dev]
       ~@body 
       )
    `(with-redefs [celestial.config/config celestial.fixtures/local-prox celestial.model/env :dev ]
       ~@(conj body f) 
       )))

(defmacro with-defaults
  "A fact that includes default conf and admin user" 
  [& args]
  `(with-admin
    (with-conf ~@args)))

(defn populate []
  (r/clear-all)
  (p/add-type redis-type)
  (p/add-action redis-actions)
  (doseq [i (range 100)] 
    (if (= 0 (mod i 2)) 
      (s/add-system redis-prox-spec)
      (s/add-system redis-ec2-spec))))

(defn add-users 
  "populates admin and ronen users" 
  []
  (p/add-user admin)
  (p/add-user ronen))

(def host (.getHostName (java.net.InetAddress/getLocalHost)))

(def ^{:doc "an ami with puppet baked in"}
  puppet-ami (merge-with merge redis-ec2-spec {:aws {:image-id "ami-f5e2ff81" :key-name host}}))
