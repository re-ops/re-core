(ns openstack.common
  "common openstack api access"
  (:require 
    [celestial.model :refer (hypervisor)]
    )
  (:import 
    org.openstack4j.openstack.OSFactory
    ) 
  )

(defn openstack [tenant]
  (let [{:keys [username password endpoint]} (hypervisor :openstack)]
    (-> (OSFactory/builder) 
        (.endpoint endpoint)
        (.credentials username password)
        (.tenantName tenant)
        (.authenticate))))

(defn compute [tenant]
  (let [{:keys [username password endpoint]} (hypervisor :openstack)]
    (.compute (openstack tenant))))

(defn servers [tenant] (-> (compute tenant) (.servers)))
 
