(ns celestial.integration.networking
  (:use midje.sweet)
  (:require 
    [celestial.fixtures :refer [with-conf]]  
    [hypervisors.networking :as n :refer (initialize-range long-to-ip list-used-ips clear-range)])
 )

(with-conf
  (with-state-changes [(before :facts (do (clear-range :proxmox) (n/initialize-networking)))]
    (fact "used up ips marking" :integration :redis :ip
      (n/gen-ip {} :proxmox :ip_address)  => {:ip_address "192.168.5.100"}
      (count (n/list-free-ips :proxmox)) => 152
      (n/list-used-ips :proxmox) => ["192.168.5.100" "192.168.5.170" "192.168.5.171" "192.168.5.173"]
      (n/release-ip "192.168.5.100" :proxmox)
      (n/list-used-ips :proxmox) => ["192.168.5.170" "192.168.5.171" "192.168.5.173"]
      (count (n/list-free-ips :proxmox)) => 153)

    (fact "multiple ips" :integration :redis :ip
      (dotimes [_ 5] (n/gen-ip {} :proxmox :ip_address))
      (count (n/list-free-ips :proxmox)) => 148
      (dotimes [i 5] (n/release-ip (str  "192.168.5.10" i) :proxmox)) 
      (count (n/list-free-ips :proxmox)) => 153)
    ))


