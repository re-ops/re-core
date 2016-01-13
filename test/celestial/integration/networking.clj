(ns celestial.integration.networking
  (:use midje.sweet)
  (:require 
    [celestial.fixtures.data :refer :all]
    [celestial.fixtures.core :refer [with-conf]]  
    [hypervisors.networking :as n :refer (initialize-range long-to-ip list-used-ips clear-range)])
 )

(with-conf
  (with-state-changes [(before :facts (do (clear-range :proxmox) (n/initialize-networking)))]
    (fact "used up ips marking" :integration :redis :ip
      (n/gen-ip {} :proxmox :ip_address)  => {:ip_address "192.168.3.200"}
      (count (n/list-free-ips :proxmox)) => 52
      (n/list-used-ips :proxmox) => ["192.168.3.200" "192.168.3.230" "192.168.3.231" "192.168.3.232"]
      (n/release-ip "192.168.3.200" :proxmox)
      (n/list-used-ips :proxmox) => ["192.168.3.230" "192.168.3.231" "192.168.3.232"]
      (count (n/list-free-ips :proxmox)) => 53)

    (fact "multiple ips" :integration :redis :ip
      (dotimes [_ 5] (n/gen-ip {} :proxmox :ip_address))
      (count (n/list-free-ips :proxmox)) => 48
      (dotimes [i 5] (n/release-ip (str  "192.168.3.20" i) :proxmox)) 
      (count (n/list-free-ips :proxmox)) => 53)
    ))


