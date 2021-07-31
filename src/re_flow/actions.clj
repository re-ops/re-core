(ns re-flow.actions
  (:require
   [clojure.core.strint :refer (<<)]
   [me.raynes.fs :refer (mkdir exists?)]
   [re-mote.repl.base :refer (scp-from scp-into)]
   [re-mote.zero.certs :refer (refer-certs)]
   [re-mote.zero.nebula :refer (refer-nebula)]
   [re-mote.zero.service :refer (refer-service)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e)]))

(refer-timbre)
(refer-certs)
(refer-nebula)
(refer-service)

(defn download-?e [?e [src dest]]
  (debug "downloading" src dest)
  (run-?e scp-from (assoc ?e :pick-by :ip) src dest))

(defn upload-?e [?e [file dest]]
  (debug "uploading" file "into" dest)
  (run-?e scp-into (assoc ?e :pick-by :ip) file dest))

(defn restart-?e [?e [service]]
  (debug "restarting" service)
  (run-?e srv/restart (assoc ?e :pick-by :ip) service))

(defn nebula-sign-?e [?e [name ip groups crt key dest]]
  (run-?e sign- ?e name ip groups crt key dest))

(def actions
  (atom
   {:re-flow.certs/set-domain (fn [?e _] (run-?e set-domains ?e (into [] (keys (?e :domains)))))
    :re-flow.certs/renew (fn [?e [user token]] (run-?e renew ?e user token))
    :re-flow.nebula/sign nebula-sign-?e
    :mkdir (fn [_ [dir]] (or (mkdir dir) (exists? dir)))
    :restart restart-?e
    :download download-?e
    :upload upload-?e}))

(defn run
  "Run a side effect function from within a rule"
  [action ?e & args]
  (if (not (@actions action))
    (do
      (error "failed to find" action) true)
    ((@actions action) ?e args)))
