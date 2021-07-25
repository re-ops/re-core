(ns re-flow.core
  (:require
   [re-share.core :refer (gen-uuid)]
   [re-share.core :refer (error-m)]
   [re-flow.session :refer (update- run-query)]
   [re-flow.queries :refer :all]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(defn trigger [& facts]
  (future
    (info "Triggering the following flows" (mapv :flow facts))
    (with-open [file (clojure.java.io/writer (java.io.File/createTempFile "flow-" ".out"))]
      (binding [*out* file]
        (try
          (update- facts)
          (debug "Finished firing rules")
          (catch Exception e
            (error-m e)))))))

(comment
  (trigger {:state :re-flow.certs/start :flow :re-flow.certs/certs})
  (trigger {:state :re-flow.nebula/start
            :flow :re-flow.nebula/sign
            :certs "/datastore/code/re-ops/re-core/certs"
            :intermediary "/tmp/nebula-certs"
            :dest "/tmp/"
            :range "192.168.100.0"
            :hosts [{:hostname "instance-2" :groups ["servers"]}
                    {:hostname "instance-1" :groups ["trusted" "laptops"]}]})
  (run-query get-provisioned)
  (run-query get-success)
  (run-query get-failures))
