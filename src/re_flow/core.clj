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
  (run-query get-provisioned)
  (run-query get-success)
  (run-query get-failures))
