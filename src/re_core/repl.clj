(ns re-core.repl
  "Repl Driven re-core "
   (:require
     [re-core.security :refer  (set-user)]
     [re-core.repl.base :refer (refer-base)]
     [re-core.repl.systems :refer (refer-systems)]
     [taoensso.timbre  :as timbre :refer (set-level!)])
   (:import
     [re-core.repl.base Types Systems]))

(refer-base)
(refer-systems)

(set-level! :debug)

(def systems (Systems.))

(def types (Types.))

(defn stop-by-grep [sys]
  (set-user {:username "admin"} (run (ls sys) | (grep :os :ubuntu-14.04) | (stop) | (watch))))

(defn list-by-grep [sys]
  (run (ls sys) | (grep :os :ubuntu-14.04) | (pretty)))

(defn create-ec2 [sys]
  (let [base (clojure.edn/read-string (slurp "data/resources/redis-ec2-system.edn"))
        specs (map (fn [i] (assoc-in base [:machine :hostname] (str "red" i))) (range 2))]
    (run (add sys specs) | (create) | (watch))))

