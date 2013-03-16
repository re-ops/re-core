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
    [celestial.fixtures :only (redis-prox-spec local-prox redis-type)]
    [celestial.puppet-standalone :only (copy-module)]))


(deftest ^:puppet redis-provision
  (with-redefs [config local-prox]
    (reload redis-prox-spec) 
    (puppetize redis-type redis-prox-spec)))



