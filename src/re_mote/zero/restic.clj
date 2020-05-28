(ns re-mote.zero.restic
  "Restic https://restic.net/ operations"
  (:require
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-cog.scripts.restic :as restic]
   [re-mote.zero.pipeline :refer (run-hosts)]
   re-mote.repl.base)

  (:import [re_mote.repl.base Hosts]))

(defprotocol Restic
  (backup [this bckp timeout])
  (check [this bckp timeout])
  (restore [this bckp dest timeout callback]))

(extend-type Hosts
  Restic
  (check [this bckp timeout]
    [this (run-hosts this shell (shell-args (restic/check bckp)) timeout)])
  (backup [this bckp timeout]
    [this (run-hosts this shell (shell-args (restic/backup bckp)) timeout)])
  (restore [this bckp dest timeout callback]
    [this (run-hosts this shell (shell-args (restic/restore bckp dest)) timeout callback)]))

(defn refer-restic []
  (require '[re-mote.zero.restic :as rst :refer (backup check restore)]))
