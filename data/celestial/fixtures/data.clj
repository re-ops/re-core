(ns celestial.fixtures.data
  "loading fixtures data"
  (:require 
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
 
(def redis-prox-spec (read-fixture "redis-system"))

(def redis-bridged-prox (read-fixture "redis-system-bridged"))

(def redis-type (read-fixture "redis-type"))

(def smokeping-type (read-fixture "smokeping-type"))

(def redis-openstack
  (assoc-in (read-fixture "redis-openstack") [:openstack :key-name] host))

(def redis-ec2-spec 
  (assoc-in (read-fixture "redis-ec2-system") [:aws :key-name] host))

(def redis-ec2-centos 
  (assoc-in (read-fixture "redis-ec2-centos") [:aws :key-name] host))

(def redis-physical (read-fixture "redis-physical"))

(def redis-vc-spec (read-fixture "redis-vc-system"))

(def redis-docker-spec (read-fixture "redis-docker-system"))

(def local-prox (read-fixture "celestial"))

(def proxmox-3 (read-fixture "proxmox-3"))

(def clustered-prox (read-fixture "celestial-cluster"))
 
(def redis-deploy (read-fixture "redis-deploy"))

(def redis-runall (read-fixture "redis-runall"))

(def basic-audit (read-fixture "basic-audit"))

(def user-quota (read-fixture "user-quota"))

(def local-conf 
  (let [path (me.raynes.fs/expand-home "~/.celestial.edn")]
    (when (me.raynes.fs/exists? path) (slurp-edn path))))
