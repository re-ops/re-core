(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.persistency :as p]
    [celestial.fixtures.core :refer (with-defaults) :as f]  
    [celestial.fixtures.data :as d]  
    [celestial.workflows :refer (reload puppetize destroy)]
    [celestial.redis :refer (clear-all)]
    [celestial.config :refer (path)]
    )
  (:use midje.sweet))

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
        (run-cycle d/redis-prox-spec d/redis-type))

  (fact "provisioning a vcenter instance" :integration :puppet :vcenter
        (run-cycle d/redis-vc-spec d/redis-type)) 

  (fact "provisioning an ec2 instance" :integration :puppet :ec2
        "assumes a working ec2 defs in ~/.celestial.edn"
        path => truthy
        (run-cycle d/redis-ec2-spec d/redis-type)) 

  (fact "ec2 with s3 source url type" :integration :puppet :ec2 :s3
        "assumes a working ec2 defs in ~/.celestial.edn"
        (let [s3-redis (assoc-in d/redis-type [:puppet-std :module :src] "s3://opsk-sandboxes/redis-sandbox-0.3.4.tar.gz")]
          path => truthy
          (run-cycle d/redis-ec2-spec s3-redis)))) 
