(ns aws.networking
  "AWS networking functions"
  (:require 
    [aws.sdk.ec2 :as ec2]
    [slingshot.slingshot :refer  [throw+]] 
    [celestial.model :refer (hypervisor)]  
    [supernal.sshj :refer (execute)] 
    [clojure.core.strint :refer (<<)] 
    [clojure.string :refer (join)]
    [aws.common :refer (with-ctx instance-desc)]
    [celestial.persistency.systems :as s]))

(defn pub-dns [endpoint instance-id]
  (instance-desc endpoint instance-id :public-dns))

(defn pubdns-to-ip
  "Grabs public ip from dnsname ec2-54-216-121-122.eu-west-1.compute.amazonaws.com"
  [pubdns]
  (join "." (rest (re-find #"ec2\-(\d+)-(\d+)-(\d+)-(\d+).*" pubdns))))

(defn update-ip [spec endpoint instance-id]
  "updates public dns in the machine persisted data"
  (when (s/system-exists? (spec :system-id))
    (let [ec2-host (pub-dns endpoint instance-id)]
      (s/partial-system (spec :system-id) {:machine {:ip (pubdns-to-ip ec2-host)}}))))

(defn override-hostname 
  "sets hostname and hosts file" 
  [hostname fqdn remote]
  (execute (<< "echo ~{hostname} | sudo tee /etc/hostname") remote )
  (execute (<< "echo 127.0.1.1 ~{fqdn} ~{hostname} | sudo tee -a /etc/hosts") remote)) 

(defn kernel-hostname
  "Set hosname in kernel for all OSes" 
  [hostname fqdn remote]
  (execute (<< "echo kernel.hostname=~{hostname} | sudo tee -a /etc/sysctl.conf") remote )
  (execute (<< "echo kernel.domainname=\"~{fqdn}\" | sudo tee -a /etc/sysctl.conf") remote )
  (execute "sudo sysctl -e -p" remote))

(defn redhat-hostname
  "Sets up hostname under /etc/sysconfig/network in redhat based systems" 
  [fqdn remote]
  (execute 
    (<< "grep -q '^HOSTNAME=' /etc/sysconfig/network && sudo sed -i 's/^HOSTNAME=.*/HOSTNAME=~{fqdn}' /etc/sysconfig/network || sudo sed -i '$ a\\HOSTNAME=~{fqdn}' /etc/sysconfig/network") remote )
  )

(defn set-hostname [{:keys [aws machine] :as spec} endpoint instance-id user]
  "Uses a generic method of setting hostname in Linux (see http://www.debianadmin.com/manpages/sysctlmanpage.txt)
  Note that in ec2 both Centos and Ubuntu use sudo!"
  (let [{:keys [hostname domain os]} machine  fqdn (<< "~{hostname}.~{domain}")
        remote {:host (pub-dns endpoint instance-id) :user user}]
    (kernel-hostname hostname fqdn remote)
    (override-hostname hostname fqdn remote)
    (case (hypervisor :aws :ostemplates os :flavor)
      :debian  true ; hothing special todo
      :redhat  (redhat-hostname fqdn remote)
      (throw+ ::no-matching-flavor :msg (<< "no os flavor found for ~{os}")))
    (with-ctx ec2/create-tags [(instance-desc endpoint instance-id :id)] {:Name hostname})
    ))
