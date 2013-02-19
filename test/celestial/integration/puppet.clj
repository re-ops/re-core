(ns celestial.integration.puppet
  "Requires both proxmox redis and a celetial instance to reply to puppet ext queries"
  (:use 
    clojure.test    
    [celestial.api :only (app)]
    [ring.adapter.jetty :only (run-jetty)] 
    [celestial.tasks :only (reload puppetize)]
    [celestial.common :only (config)]
    [celestial.puppet-standalone :only (copy-module)]))

;

(def baseline
  {:system
   {:vmid 33 :cpus  2 :memory 1024 :hostname "redis-test" :disk 30
    :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
    :ip_address "192.168.5.33" :password "foobar1" :nameserver "8.8.8.8"
    :hypervisor "proxmox"
    }
   :provision
   {:module  {:name "redis-sandbox-0.3.1" 
              :src "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.1.tar.gz"}
    :server {:host "192.168.5.33"}}
   }
  )

(def local-prox 
  {:hypervisor  {:username "root" :password "foobar" :host "localhost" :ssh-port 22222}})

(alter-var-root (var config) (fn [old] local-prox))

(deftest ^:puppet redis-provision
  (reload baseline) 
  (puppetize (:provision baseline)))
