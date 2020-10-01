(ns re-mote.zero.certs
  (:require
   re-mote.repl.base
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]
   [re-cog.scripts.letsencrypt :refer (update-certs)])
  (:import re_mote.repl.base.Hosts))

(refer-out)
(refer-base)

(defprotocol Certs
  (renew [this user token]))

(extend-type Hosts
  Certs
  (renew [this user token]
    [this (run-hosts this shell (shell-args (update-certs user token)) [5 :minute])]))

(defn ^{:category :security} cert-renew
  "Cert renewal"
  [hs user token]
  (run> (renew hs user token) | (pretty "cert renwal")))

(defn refer-certs []
  (require '[re-mote.zero.certs :as crt :refer (renew)]))
