(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:require 
    [celestial.persistency :as p])
  (:use 
    midje.sweet
    [clojure.java.io :only (file)]
    [taoensso.timbre :only (debug info error warn)]
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.workflows :only (reload puppetize destroy)]
    [celestial.config :only (path)]
    [celestial.redis :only (clear-all)]
    [celestial.fixtures :only (with-conf redis-prox-spec redis-ec2-spec redis-vc-spec redis-type host puppet-ami)]))

(defn run-cycle [spec type]
  (clear-all) 
  (p/add-type type) 
  (let [id (p/add-system spec)] 
    (try 
      (reload (assoc spec :system-id id))
      (puppetize type (p/get-system id))
      (finally 
        (destroy (assoc (p/get-system id) :system-id id))))))

(fact "provisioning a proxmox instance" :integration :puppet :proxmox
      (with-conf
        (run-cycle redis-prox-spec redis-type)))

(fact "provisioning a vcenter instance" :integration :puppet :vcenter
      (with-conf
        (run-cycle redis-vc-spec redis-type)))

(fact "provisioning an ec2 instance" :integration :puppet :ec2
      "assumes a working ec2 defs in ~/.celestial.edn"
       path => truthy
      (run-cycle puppet-ami redis-type))

(fact "ec2 with s3 source url type" :integration :puppet :ec2 :s3
      "assumes a working ec2 defs in ~/.celestial.edn"
      (let [s3-redis (assoc-in redis-type [:puppet-std :module :src] "s3://opsk-sandboxes/redis-sandbox-0.3.4.tar.gz")]
        path => truthy
        (run-cycle puppet-ami s3-redis)))
