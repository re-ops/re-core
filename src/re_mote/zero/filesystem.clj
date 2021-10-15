(ns re-mote.zero.filesystem
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.resources.file :as f :refer (directory)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Filesystem
  (rmdir
    [this d]
    [this d m]))

(extend-type Hosts
  Filesystem
  (rmdir
    ([this d]
     [this (run-hosts this f/directory [d :absent] [10 :second])])
    ([this d _]
     (rmdir this d))))
