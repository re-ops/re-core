(ns re-mote.zero.devices
  (:require
   re-mote.repl.base
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.facts.datalog :refer (run-query)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import re_mote.repl.base.Hosts))

(defprotocol Devices
  (usb [this] [this m]))

(extend-type Hosts
  Devices
  (usb
    ([this]
     [this (run-hosts this run-query  ['[:find ?v :where [_ :usb-devices/connected-devices.name ?v]]] [5 :second])])
    ([this _]
     (usb this))))

(defn refer-devices []
  (require '[re-mote.zero.devices :as devices :refer (usb)]))
