(ns celestial.integration.networking
  (:use midje.sweet)
  (:require 
    [celestial.redis :refer [clear-all]]
    [celestial.fixtures :refer [with-conf]]  
    [hypervisors.networking :as n :refer (initialize-range long-to-ip list-used-ips)])
 )

(with-state-changes [(before :facts (clear-all))]
  (fact "used up ips marking" :integration :redis :ip
    (let [k "proxmox:dev"]
       (with-conf
         (n/gen-ip {} k)  => {:ip_address "192.168.5.100"}
         (n/list-used-ips k) => ["192.168.5.100" "192.168.5.170" "192.168.5.171" "192.168.5.173"]
         (n/release-ip "192.168.5.100" k)
         (n/list-used-ips k) => ["192.168.5.170" "192.168.5.171" "192.168.5.173"]))))
