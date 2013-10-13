(ns celestial.test.hooks
  "Testing misc hooks like dns and hubot"
  (:require 
    [celestial.persistency.systems :as s] 
    [celestial.persistency :as p])
  (:use 
     midje.sweet
    [hooks.dnsmasq :only (add-host remove-host hostline)]
    [supernal.sshj :only (execute)] 
    ))

(fact "machine domain precedence" 
     (add-host {:system-id 1 :domain "remote" :dnsmasq "192.168.1.1" :user "foo"}) => nil
     (provided 
       (execute 
         "grep -q '1.2.3.4 iobar iobar.local' /etc/hosts || (echo '1.2.3.4 iobar iobar.local' | sudo tee -a /etc/hosts >> /dev/null)" 
           {:host "192.168.1.1", :user "foo"} ) => nil :times 1
       (execute "sudo service dnsmasq stop && sudo service dnsmasq start"  {:host "192.168.1.1", :user "foo"}) => nil :times 1
       (s/get-system 1) => {:machine {:domain "local" :ip "1.2.3.4" :hostname "iobar"}}))

(fact "domainless machine" 
     (add-host {:system-id 1 :domain "remote" :dnsmasq "192.168.1.1" :user "foo"}) => nil
     (provided 
       (execute 
         "grep -q '1.2.3.4 iobar iobar.remote' /etc/hosts || (echo '1.2.3.4 iobar iobar.remote' | sudo tee -a /etc/hosts >> /dev/null)" 
           {:host "192.168.1.1", :user "foo"} ) => nil :times 1
       (execute "sudo service dnsmasq stop && sudo service dnsmasq start"  {:host "192.168.1.1", :user "foo"}) => nil :times 1
       (s/get-system 1) => {:machine {:ip "1.2.3.4" :hostname "iobar"} }))

