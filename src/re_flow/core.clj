(ns re-flow.core
  (:require
   re-flow.setup
   re-flow.restore
   [re-share.core :refer (error-m)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(defquery get-failures
  "Find all failures"
  []
  [?f <- ::state (= true (this :failure))])

(defquery get-success
  "Find all failures"
  []
  [?f <- ::state (or (nil? (this :failure)) (not (this :failure)))])

(defquery get-provisioned
  "Find provisioned hosts"
  []
  [?p <- :re-flow.setup/provisioned])

(def session (atom (mk-session 're-flow.core 're-flow.setup 're-flow.restore :fact-type-fn :state)))

(defn trigger [& facts]
  (future
    (info "Starting to insert facts")
    (try
      (when-let [initialized-session (reduce (fn [s fact] (insert s fact)) @session facts)]
        (reset! session (fire-rules initialized-session))
        (info "Finished firing rules"))
      (catch Exception e
        (error-m e)))))

(comment
  (trigger {:state :re-flow.restore/start :flow :re-flow.restore/restore})
  (query @session get-provisioned)
  (query @session get-success)
  (query @session get-failures))
