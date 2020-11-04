(ns re-mote.zero.certs
  (:require
   re-mote.repl.base
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]
   [re-cog.scripts.letsencrypt :refer (update-certs apply-domains)])
  (:import re_mote.repl.base.Hosts))

(refer-out)
(refer-base)

(defprotocol Certs
  (set-domains [this domains])
  (renew [this user token]))

(extend-type Hosts
  Certs
  (set-domains [this domains]
    [this (run-hosts this shell (shell-args (apply-domains domains)) [5 :second])])
  (renew [this user token]
    [this (run-hosts this shell (shell-args (update-certs user token)) [5 :minute])]))

(defn ^{:category :security} cert-renew
  "Cert renewal"
  [hs user token]
  (run> (renew hs user token) | (pretty "cert renwal")))

(defn ^{:category :security} setup-domains
  "Cert domains setup"
  [hs domains]
  (run> (set-domains hs domains) | (pretty "domain certs set")))

(defn refer-certs []
  (require '[re-mote.zero.certs :as crt :refer (renew setup-domains)]))
