(ns re-flow.demo
  "Example flows"
  (:require
   [re-flow.core :refer (trigger)]))

(defn nebula-signing []
  (trigger {:state :re-flow.nebula/start
            :flow :re-flow.nebula/sign
            :certs "/datastore/code/re-ops/re-core/certs"
            :intermediary "/tmp/nebula-certs"
            :sign-dest "/tmp/"
            :deploy-dest "/usr/local/etc/nebula"
            :hosts [{:hostname "lighthouse" :groups ["lighthouse"] :ip "192.168.100.1/24"}
                    {:hostname "instance-2" :groups ["servers"] :ip "192.168.100.2/24"}
                    {:hostname "instance-1" :groups ["trusted" "laptops"] :ip "192.168.100.3/24"}]}))

(defn letsencrypt []
  (trigger {:state :re-flow.certs/start :flow :re-flow.certs/certs}))
