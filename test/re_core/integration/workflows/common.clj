(ns re-core.integration.workflows.common
  "Common testing workflows fns"
  (:require
   [re-core.persistency.systems :as s]))

(defn spec
  ([] (spec {}))
  ([m] (assoc (merge-with merge (s/get-system 1) m) :system-id 1)))

(defn get-spec [& ks]
  (get-in (spec) ks))
