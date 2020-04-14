(ns re-mote.zero.disk
  "Disk manipulation"
  (:require
   [re-cog.scripts.common :refer (shell-args)]
   [re-cog.scripts.disks :as d]
   [re-cog.resources.exec :refer (shell)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol Disk
  (partition-
    [this device])
  (mount
    [this device target]))

(extend-type Hosts
  Disk
  (partition- [this device]
    [this (run-hosts this shell (shell-args (d/partition- device)) [1 :minute])])
  (mount [this device target]
    [this (run-hosts this shell (shell-args (d/mount device target)) [5 :second])]))

(defn refer-disk []
  (require '[re-mote.zero.disk :as disk]))
