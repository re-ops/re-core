(ns re-flow.core
  (:require
   [re-share.core :refer (error-m)]
   [re-flow.session :refer (update- run-query)]
   [re-flow.queries :refer :all]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(defn trigger [& facts]
  (future
    (info "Starting to insert facts")
    (try
      (update- facts)
      (info "Finished firing rules")
      (catch Exception e
        (error-m e)))))

(comment
  (trigger {:state :re-flow.restore/start :flow :re-flow.restore/restore})
  (run-query get-provisioned)
  (run-query get-success)
  (run-query get-failures))
