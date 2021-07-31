(ns re-mote.zero.service
  "control a service"
  (:require
   [re-cog.resources.service :as c]
   [re-mote.zero.pipeline :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Service
  (restart
    [this service-name]
    [this m service-name]))

(extend-type Hosts
  Service
  (restart
    ([this service-name]
     (restart this {} service-name))
    ([this m service-name]
     [this (run-hosts this c/service [service-name :restart])])))

(defn refer-service []
  (require '[re-mote.zero.service :as srv]))
