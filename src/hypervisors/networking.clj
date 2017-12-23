(ns hypervisors.networking
  "Common hypervizors networking logic"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.ssh.transport :refer (execute)]
   [selmer.filters :refer (add-filter!)]
   [clojure.core.strint :refer [<<]]
   [selmer.parser :refer (render-file)]
   [slingshot.slingshot :refer [throw+ try+]]))

(refer-timbre)

(add-filter! :not-empty? (comp not empty?))

(defn debian-interfaces
  "Generates a static ip template"
  [config]
  (render-file "interfaces.slem" config))

(defn redhat-network-cfg [config]
  (render-file "network.slem" config))

(defn redhat-ifcfg-eth0 [config]
  (render-file "ifcfg-eth0.slem" config))

(defn override-hostname
  "sets hostname and hosts file"
  [hostname fqdn remote]
  (execute (<< "echo ~{hostname} | sudo tee /etc/hostname") remote)
  (execute (<< "echo 127.0.1.1 ~{fqdn} ~{hostname} | sudo tee -a /etc/hosts") remote))

(defn kernel-hostname
  "Set hosname in kernel for all OSes"
  [hostname fqdn remote]
  (execute (<< "echo kernel.hostname=~{hostname} | sudo tee -a /etc/sysctl.conf") remote)
  (execute (<< "echo kernel.domainname=\"~{fqdn}\" | sudo tee -a /etc/sysctl.conf") remote)
  (execute "sudo sysctl -e -p" remote))

(defn redhat-hostname
  "Sets up hostname under /etc/sysconfig/network in redhat based systems"
  [fqdn remote]
  (execute
   (<< "grep -q '^HOSTNAME=' /etc/sysconfig/network && sudo sed -i 's/^HOSTNAME=.*/HOSTNAME=~{fqdn}' /etc/sysconfig/network || sudo sed -i '$ a\\HOSTNAME=~{fqdn}' /etc/sysconfig/network") remote))

(defn set-hostname
  [hostname fqdn remote flavor]
  (kernel-hostname hostname fqdn remote)
  (override-hostname hostname fqdn remote)
  (case flavor
    :debian  true ; nothing special todo
    :redhat  (redhat-hostname fqdn remote)
    (throw+ {:type ::no-matching-flavor} (<< "no os flavor found for ~{flavor}"))))
