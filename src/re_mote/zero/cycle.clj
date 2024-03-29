(ns re-mote.zero.cycle
  "Managing all zero related components"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.zero.common :refer  (context close!)]
   [re-share.wait :refer  (enable-waits stop-waits)]
   [re-mote.zero.management :as mgmt]
   [re-mote.zero.callback :as clb]
   [re-mote.zero.results :as res]
   [re-mote.zero.events :refer (handle watch-misses)]
   [re-mote.zero.server :as srv]
   [re-share.zero.events :as evn]
   [re-mote.zero.send :as snd]
   [re-mote.zero.worker :as wrk]
   [mount.core :as mount :refer (defstate)])
  (:import
   org.zeromq.ZMQ
   org.zeromq.ZMQ$Context))

(refer-timbre)

(def ctx (atom nil))

(defn start []
  (reset! ctx (context))
  (snd/start)
  (srv/start @ctx ".curve/server-private.key")
  (evn/start @ctx handle)
  (wrk/start @ctx 4)
  (enable-waits)
  (clb/callback-watch)
  (res/prune-watch)
  (watch-misses))

(defn stop []
  (stop-waits)
  (snd/stop)
  (when @ctx
    (wrk/stop)
    (srv/stop @ctx)
    (evn/stop)
    (future
      (debug "terminating ctx")
      (let [c @ctx]
        (reset! ctx nil)
        (.term c)))
    (info "terminated ctx"))
  (res/clear-results)
  (mgmt/clear-registered))

(defstate ^{:on-reload :noop} zero
  :start (start)
  :stop (stop))
