(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:require 
    [celestial.persistency :as p])
  (:use 
    clojure.test    
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.tasks :only (reload puppetize)]
    [celestial.common :only (config)]
    [celestial.redis :only (clear-all)]
    [celestial.persistency :only (host register-host new-type)]  
    [celestial.fixtures :only (redis-prox-spec redis-ec2-spec local-prox redis-type)]
    [celestial.puppet-standalone :only (copy-module)]))

(deftest ^:puppet redis-ec2-provision
  "assumes a working ec2 defs in ~/.celestial.edn"
  (let [puppet-ami (assoc-in redis-ec2-spec [:aws :image-id] "ami-4eb1ba3a")
        hostname (get-in redis-ec2-spec [:machine :hostname])]
    (clear-all)
    (new-type "redis" redis-type)
    (register-host redis-ec2-spec)
    (reload puppet-ami) 
    (puppetize redis-type (host hostname))))

#_(deftest ^:puppet redis-prox-provision
  (with-redefs [config local-prox]
    (reload redis-prox-spec) 
    (puppetize redis-type redis-prox-spec)))


