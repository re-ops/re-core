(ns hypervisors.networking
  "Common hypervizors networking logic"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.ssh.transport :refer (execute)]
   [re-cog.scripts.hostname :refer (hostnamectl)]))

(refer-timbre)

(defn set-hostname
  [hostname fqdn remote flavor]
  (execute (hostnamectl hostname fqdn) remote))

(defn ssh-able? [flavor]
  (#{:redhat :debian} flavor))
