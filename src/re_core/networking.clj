(ns re-core.networking
  "Common systems networking logic"
  (:refer-clojure :exclude [list])
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.repl :refer (with-ids list)]
   [re-share.wait :refer (wait-for)])
  (:import clojure.lang.ExceptionInfo))

(refer-timbre)

(defn ips-available [ids]
  (try
    (wait-for {:timeout [1 :minute] :sleep [1000 :ms]}
              (fn []
                (let [systems (-> (list (with-ids ids) :systems :print? false) second :systems)]
                  (and
                   (not (empty? systems))
                   (every? (comp not nil? :ip :machine second) systems))))
              "Failed to wait for ips to become available")
    true
    (catch ExceptionInfo _ false)))
