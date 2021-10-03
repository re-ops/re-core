(ns re-flow.queries
  "Common flow queries"
  (:require
   [clara.rules :refer (defquery)]))

(defquery get-failures
  "Find all failures"
  []
  [?f <- :re-flow.core/state (= true (this :failure))])

(defquery get-success
  "Find all failures"
  []
  [?f <- :re-flow.core/state (or (nil? (this :failure)) (not (this :failure)))])

(defquery get-provisioned
  "Find provisioned hosts"
  []
  [?p <- :re-flow.setup/provisioned])

(defquery get-all
  "Get all facts"
  []
  [?p <- :re-flow.core/state])
