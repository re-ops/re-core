(ns re-flow.actions
  (:require
   [clojure.core.strint :refer (<<)]
   [me.raynes.fs :refer (mkdir)]
   [re-mote.repl.base :refer (scp-from scp-into)]
   [re-mote.zero.certs :refer (refer-certs)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e run-?e-non-block results failure? successful-ids)]))

(refer-timbre)
(refer-certs)

(defn cert-scp-?e [?e [domain file]]
  (debug "copying" (<< "/srv/dehydrated/certs/~{domain}/~{file}"))
  (run-?e scp-from (assoc ?e :pick-by :ip) (<< "/srv/dehydrated/certs/~{domain}/~{file}") "/tmp/certs/"))

(def actions
  (atom
   {:re-flow.certs/set-domain (fn [?e _] (run-?e set-domains ?e (?e :domains)))
    :re-flow.certs/renew (fn [?e [user token]] (run-?e renew ?e user token))
    :re-flow.certs/mkdir (fn [_ [dir]] (mkdir "/tmp/certs"))
    :re-flow.certs/scp cert-scp-?e}))

(defn run
  "Run a side effect function from within a rule"
  [action ?e & args]
  {:pre [(@actions action)]}
  ((@actions action) ?e args))
