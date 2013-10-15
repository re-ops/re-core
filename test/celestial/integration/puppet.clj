(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.persistency :as p]
    [celestial.fixtures :refer (with-defaults) :as f]  
    )
  (:use 
    midje.sweet
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (debug info error warn)]
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.workflows :only (reload puppetize destroy)]
    [celestial.config :only (path)]
    [celestial.redis :only (clear-all)]
    ))

(defn run-cycle [spec type]
  (clear-all) 
  (p/add-type type) 
  (let [id (s/add-system spec)] 
    (try 
      (reload (assoc spec :system-id id))
      (puppetize type (s/get-system id))
      (finally 
        (destroy (assoc (s/get-system id) :system-id id))))))

(with-defaults
  (fact "provisioning a proxmox instance" :integration :puppet :proxmox
        (run-cycle f/redis-prox-spec f/redis-type))

  (fact "provisioning a vcenter instance" :integration :puppet :vcenter
        (run-cycle f/redis-vc-spec f/redis-type)) 

  (fact "provisioning an ec2 instance" :integration :puppet :ec2
        "assumes a working ec2 defs in ~/.celestial.edn"
        path => truthy
        (run-cycle f/puppet-ami f/redis-type)) 

  (fact "ec2 with s3 source url type" :integration :puppet :ec2 :s3
        "assumes a working ec2 defs in ~/.celestial.edn"
        (let [s3-redis (assoc-in f/redis-type [:puppet-std :module :src] "s3://opsk-sandboxes/redis-sandbox-0.3.4.tar.gz")]
          path => truthy
          (run-cycle f/puppet-ami s3-redis)))) 
