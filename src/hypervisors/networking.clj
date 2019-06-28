(ns hypervisors.networking
  "Common hypervizors networking logic"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.ssh.transport :refer (execute)]
   [re-cog.scripts.hostname :refer (kernel-hostname override-hostname redhat-hostname)]
   [clojure.core.strint :refer [<<]]))

(refer-timbre)

(defn set-hostname
  [hostname fqdn remote flavor]
  (execute (kernel-hostname hostname fqdn) remote)
  (execute (override-hostname hostname fqdn) remote)
  (case flavor
    :debian  true ; nothing special todo
    :redhat (execute (redhat-hostname fqdn) remote)
    (throw (ex-info (<< "no os flavor found for ~{flavor}") {:flavor flavor}))))

(defn ssh-able? [flavor]
  (#{:redhat :debian} flavor))
