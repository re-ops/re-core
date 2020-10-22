(ns re-mote.zero.restic
  "Restic https://restic.net/ operations"
  (:require
   re-mote.repl.base
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-cog.scripts.restic :as restic]
   [re-mote.zero.pipeline :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Restic
  (backup [this bckp timeout])
  (unlock [this bckp timeout])
  (check [this bckp timeout])
  (restore [this bckp dest timeout callback]))

(extend-type Hosts
  Restic
  (check [this bckp timeout]
    [this (run-hosts this shell (shell-args (restic/backup bckp)) timeout)])
  (backup [this bckp timeout]
    [this (run-hosts this shell (shell-args (restic/backup bckp)) timeout)])
  (unlock [this bckp timeout]
    [this (run-hosts this shell (shell-args (restic/unlock bckp)) timeout)])
  (restore [this bckp dest timeout callback]
    [this (run-hosts this shell (shell-args (restic/restore bckp dest)) timeout callback)]))

(defn refer-restic []
  (require '[re-mote.zero.restic :as rst :refer (backup check restore)]))
