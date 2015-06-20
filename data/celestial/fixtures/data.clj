(ns celestial.fixtures.data
  "loading fixtures data"
  (:require 
    [clojure.java.io :refer (file)]
    [celestial.model :refer (operations)]
    [clojure.core.strint :refer  (<<)]
    [celestial.common :refer (slurp-edn)]
    [celestial.fixtures.core :refer (host)]))

(def admin {
  :username "admin" :password "foo"
  :envs [:dev :qa :prod] 
  :roles #{:celestial.roles/admin} 
  :operations operations
})

(def ronen {
  :username "ronen" :password "bar"
  :envs [:dev] :roles #{:celestial.roles/user} 
  :operations operations
})

(def foo {
  :username "foo" :password "bla"
  :envs [] :roles #{:celestial.roles/user} 
  :operations operations
})

(defn read-fixture [fixture]
  (slurp-edn (<< "data/resources/~{fixture}.edn")))
 
(defn load-fixtures 
   "load all fixture files" 
   []
  (doseq [f (filter #(.isFile %) (file-seq (file "data/resources")))]
    (intern *ns*  (symbol (.replace (.getName f) ".edn" "")) (slurp-edn f))))

(load-fixtures)

(def redis-prox-spec (read-fixture "redis-system"))

(def redis-bridged-prox (read-fixture "redis-system-bridged"))

(def redis-type (read-fixture "redis-type"))

(def smokeping-type (read-fixture "smokeping-type"))

(def redis-openstack-spec
  (assoc-in (read-fixture "redis-openstack") [:openstack :key-name] host))

(def redis-ec2-spec 
  (assoc-in (read-fixture "redis-ec2-system") [:aws :key-name] host))

(def redis-ec2-centos 
  (assoc-in (read-fixture "redis-ec2-centos") [:aws :key-name] host))

(def redis-vc-spec (read-fixture "redis-vc-system"))

(def redis-docker-spec (read-fixture "redis-docker-system"))

(def local-prox (read-fixture "celestial"))

(def clustered-prox (read-fixture "celestial-cluster"))
 
(def local-conf 
  (let [path (me.raynes.fs/expand-home "~/.celestial.edn")]
    (when (me.raynes.fs/exists? path) (slurp-edn path))))
