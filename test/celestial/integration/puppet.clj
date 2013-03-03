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
    [celestial.fixtures :only (spec local-prox redis-type)]
    [celestial.puppet-standalone :only (copy-module)]))


(deftest ^:puppet redis-provision
  (with-redefs [config local-prox]
    (reload spec) 
    (p/new-type "red1" redis-type) 
    (p/register-host "red1" "redis" spec) 
    (run-jetty app {:port 8080 :join? false})
    (puppetize redis-type spec)))
