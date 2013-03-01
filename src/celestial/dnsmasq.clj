(ns celestial.dnsmasq
  "A basic dnsmasq registration api for static addresses using hosts file, 
   expects an Ubuntu and dnsmasq on the other end "
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.ssh :only (execute step)]))

(def restart (step :restart-service "sudo service dnsmasq restart"))

(defn add-host [{:keys [hostname ip_address dnsmasq]}]
  (execute dnsmasq 
      (step :add-host (<< "echo '~{ip_address} ~{hostname}' | sudo tee -a /etc/hosts >>/dev/null" ))
       restart 
      ))


(defn remove-host [{:keys [hostname ip_address dnsmasq]}]
  (execute dnsmasq 
    (step :remove-host (<< "sudo sed -ie \"\\|^~{ip_address} ~{hostname}\\$|d\" /etc/hosts"))
     restart
        ))

; (add-host "foo" "192.168.20.90")
; (remove-host "foo" "192.168.20.90")
