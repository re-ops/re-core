(ns re-mote.zero.nebula
  (:require
   re-mote.repl.base
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]
   [re-cog.scripts.nebula :refer (sign)])
  (:import re_mote.repl.base.Hosts))

(refer-out)
(refer-base)

(defprotocol Nebula
  (sign- [this name ip groups]))

(extend-type Hosts
  Nebula
  (sign- [this name ip groups]
    [this (run-hosts this shell (shell-args (sign name ip (clojure.string/join "," groups))) [5 :second])]))

(defn refer-nebula []
  (require '[re-mote.zero.nebula :as neb :refer (sign-)]))
