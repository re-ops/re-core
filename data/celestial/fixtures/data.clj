(ns celestial.fixtures.data
  "loading fixtures data"
  (:require 
    [celestial.common :refer (slurp-edn)]
    [celestial.fixtures.core :refer (host)]))

(def admin {:envs [:dev :qa :prod] :roles #{:celestial.roles/admin} :username "admin" :password "foo"})

(def ronen {:envs [:dev] :roles #{:celestial.roles/user} :username "ronen" :password "bar"})
 
(def redis-prox-spec (slurp-edn "fixtures/redis-system.edn"))

(def redis-bridged-prox (slurp-edn "fixtures/redis-system-bridged.edn"))

(def redis-type (slurp-edn "fixtures/redis-type.edn"))

(def redis-ec2-spec 
  (assoc-in (slurp-edn "fixtures/redis-ec2-system.edn") [:aws :key-name] host))

(def redis-ec2-centos 
  (assoc-in (slurp-edn "fixtures/redis-ec2-centos.edn") [:aws :key-name] host))

(def redis-physical (slurp-edn "fixtures/redis-physical.edn"))

(def redis-vc-spec (slurp-edn "fixtures/redis-vc-system.edn"))

(def redis-docker-spec (slurp-edn "fixtures/redis-docker-system.edn"))

(def local-prox (slurp-edn "fixtures/celestial.edn"))

(def proxmox-3 (slurp-edn "fixtures/proxmox-3.edn"))

(def clustered-prox (slurp-edn "fixtures/celestial-cluster.edn"))
 
(def redis-deploy (slurp-edn "fixtures/redis-deploy.edn"))

(def redis-runall (slurp-edn "fixtures/redis-runall.edn"))

(def basic-audit (slurp-edn "fixtures/basic-audit.edn"))

(def user-quota (slurp-edn "fixtures/user-quota.edn"))

(def local-conf 
  (let [path (me.raynes.fs/expand-home "~/.celestial.edn")]
    (when (me.raynes.fs/exists? path) (slurp-edn path))))
