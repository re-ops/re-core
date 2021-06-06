(ns re-core.networking
  "Common systems networking logic"
  (:require
   [re-core.repl :refer (with-ids)]
   [re-share.wait :refer (wait-for)])
  (:import clojure.lang.ExceptionInfo))

(defn ips-available [ids]
  (try
    (wait-for {:timeout [1 :minute]}
              (fn []
                (let [systems (-> (list (with-ids ids) :systems :print? false) second :systems)]
                  (every? (comp not nil? :ip  :machine second) systems)))
              "Failed to wait for ips to become available")
    true
    (catch ExceptionInfo _ false)))
