(ns aws.networking
  "AWS networking functions"
  (:import com.amazonaws.services.ec2.model.AssociateAddressRequest)
  (:require
   [hypervisors.networking :as net]
   [taoensso.timbre :as timbre]
   [amazonica.aws.ec2 :as ec2]
   [slingshot.slingshot :refer  [throw+]]
   [re-core.model :refer (hypervisor)]
   [re-mote.ssh.transport :refer (execute)]
   [clojure.core.strint :refer (<<)]
   [clojure.string :refer (join)]
   [aws.common :refer (with-ctx instance-desc)]
   [es.systems :as s]))

(timbre/refer-timbre)

(defn instance-ip [{:keys [aws] :as spec} endpoint instance-id]
  (let [public-ip (instance-desc endpoint instance-id :public-ip-address)
        private-ip  (instance-desc endpoint instance-id :private-ip-address)]
    (if public-ip
      public-ip
      (when (and (aws :network-interfaces) private-ip) private-ip))))

(defn update-ip
  "updates public dns in the machine persisted data"
  [{:keys [aws] :as spec} endpoint instance-id]
  (when (s/exists? (spec :system-id))
    (s/partial (spec :system-id) {:machine {:ip (instance-ip spec endpoint instance-id)}})))

(defn set-hostname
  "Uses a generic method of setting hostname in Linux
    (see http://www.debianadmin.com/manpages/sysctlmanpage.txt)
    Note that in ec2 both Centos and Ubuntu use sudo!"
  [{:keys [machine] :as spec} endpoint instance-id user]
  (let [{:keys [hostname domain os]} machine fqdn (<< "~{hostname}.~{domain}")
        remote {:host (instance-ip spec endpoint instance-id) :user user}
        flavor (hypervisor :aws :ostemplates os :flavor)]
    (net/set-hostname hostname fqdn remote flavor)
    (with-ctx ec2/create-tags
      {:resources [instance-id] :tags [{:key "Name" :value hostname}]})))

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
