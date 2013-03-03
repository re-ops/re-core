(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:use 
    clojure.test    
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.tasks :only (reload puppetize)]
    [celestial.common :only (config)]
    [celestial.fixtures :only (spec local-prox type)]
    [celestial.puppet-standalone :only (copy-module)]))


#_(deftest ^:puppet redis-provision
  (with-redefs [config local-prox]
    (reload spec) 
    (puppetize (:provision baseline))))
