(ns re-core.fixtures.data
  "loading fixtures data"
  (:require
    [me.raynes.fs :as fs]
    [clojure.java.io :refer (file)]
    [re-core.model :refer (operations)]
    [clojure.core.strint :refer  (<<)]
    [re-core.common :refer (slurp-edn)]
    [re-core.fixtures.core :refer (host)]))

(def admin {
  :username "admin" :password "foo"
  :envs [:dev :qa :prod]
  :roles #{:re-core.roles/admin}
  :operations operations
})

(def ronen {
  :username "ronen" :password "bar"
  :envs [:dev] :roles #{:re-core.roles/user}
  :operations operations
})

(def foo {
  :username "foo" :password "bla"
  :envs [] :roles #{:re-core.roles/user}
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

(def redis-kvm-spec (read-fixture "redis-system"))

(def redis-type (read-fixture "redis-type"))

(def smokeping-type (read-fixture "smokeping-type"))

(def redis-ec2-spec
  (assoc-in (read-fixture "redis-ec2-system") [:aws :key-name] host))

(def redis-ec2-centos
  (assoc-in (read-fixture "redis-ec2-centos") [:aws :key-name] host))

(def local-prox (read-fixture "re-core"))

(def local-conf
  (let [path (fs/expand-home "~/.re-core.edn")]
    (when (fs/exists? path) (slurp-edn path))))
