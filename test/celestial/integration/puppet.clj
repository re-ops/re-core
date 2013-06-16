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
    [celestial.fixtures :only (with-conf redis-prox-spec redis-ec2-spec redis-type)]))

(defn run-cycle [spec type]
    (clear-all) 
    (p/add-type type) 
    (let [id (p/add-system spec)] 
      (reload (assoc spec :system-id id))
      (puppetize type (p/get-system id))
      (destroy (assoc (p/get-system id) :system-id id))))

(fact "provisioning a proxmox instance" :integration :puppet
      (with-conf
        (run-cycle redis-prox-spec redis-type)))

#_(fact "provisioning an ec2 instance" :integration :puppet 
      "assumes a working ec2 defs in ~/.celestial.edn"
      (let [puppet-ami (assoc-in redis-ec2-spec [:aws :image-id] "ami-4eb1ba3a")]
        path => truthy
        (run-cycle puppet-ami redis-type)))




