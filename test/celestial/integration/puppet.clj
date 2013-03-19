(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:require 
    [celestial.persistency :as p])
  (:use 
    clojure.test    
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (debug info error warn)]
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.tasks :only (reload puppetize)]
    [celestial.common :only (config config-exists?)]
    [celestial.redis :only (clear-all)]
    [celestial.persistency :only (host register-host new-type)]  
    [celestial.fixtures :only (redis-prox-spec redis-ec2-spec local-prox redis-type)]))

(defn run-cycle [spec type]
  (let [hostname (get-in redis-ec2-spec [:machine :hostname])]
    (clear-all) 
    (new-type "redis" type) 
    (register-host spec) 
    (let [vm (reload spec)] 
      (puppetize type (host hostname))
      (.stop vm)
      (.delete vm))))

(deftest ^:puppet redis-ec2-provision
  "assumes a working ec2 defs in ~/.celestial.edn"
  (let [puppet-ami (assoc-in redis-ec2-spec [:aws :image-id] "ami-4eb1ba3a")]
    (assert (config-exists?))
    (run-cycle puppet-ami redis-type)))

(deftest ^:puppet redis-prox-provision
   (with-redefs [config local-prox]
     (run-cycle redis-prox-spec redis-type)))


