(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns aws.networking
  "AWS networking functions"
  (:import com.amazonaws.services.ec2.model.AssociateAddressRequest)
  (:require 
    [taoensso.timbre :as timbre]
    [amazonica.aws.ec2 :as ec2]
    [slingshot.slingshot :refer  [throw+]] 
    [celestial.model :refer (hypervisor)]  
    [supernal.sshj :refer (execute)] 
    [clojure.core.strint :refer (<<)] 
    [clojure.string :refer (join)]
    [aws.common :refer (with-ctx instance-desc)]
    [celestial.persistency.systems :as s]))

(timbre/refer-timbre)

(defn pub-dns [endpoint instance-id]
  (instance-desc endpoint instance-id :public-dns-name))

(defn update-ip [spec endpoint instance-id]
  "updates public dns in the machine persisted data"
  (when (s/system-exists? (spec :system-id))
    (let [public-ip (instance-desc endpoint instance-id :public-ip-address)]
      (s/partial-system (spec :system-id) {:machine {:ip public-ip}}))))

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
      (throw+ {:type ::no-matching-flavor} (<< "no os flavor found for ~{os}")))
    (with-ctx ec2/create-tags 
      {:resources [instance-id] :tags [{:key "Name" :value hostname}]})
    ))

(defn describe-eip [endpoint instance-id]
  (with-ctx ec2/describe-addresses :filters 
    [{:name "instance-id" :values [instance-id]}]))

(defn describe-address [endpoint ip]
   (get-in (with-ctx ec2/describe-addresses {:public-ips [ip]}) [:addresses 0]))

(defn attach-vpc-ip [endpoint instance-id {:keys [machine aws] :as spec}]
   (let [{:keys [ip]} machine {:keys [allocation-id]} (describe-address endpoint ip)]
      (with-ctx ec2/associate-address {:instance-id instance-id :allocation-id allocation-id})))

(defn assoc-pub-ip [endpoint instance-id {:keys [machine aws] :as spec}]
   (let [{:keys [ip]} machine {:keys [network-interfaces]} aws]
     (if-not network-interfaces
       (with-ctx ec2/associate-address {:instance-id instance-id :public-ip ip})
       (attach-vpc-ip endpoint instance-id spec))))


