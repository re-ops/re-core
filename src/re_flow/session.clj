(ns re-flow.session
  "Clara session managment"
  (:require
   re-flow.setup
   re-flow.queries
   re-flow.restore
   re-flow.certs
   re-flow.nebula
   re-flow.notification
   re-flow.disposable
   re-flow.dashboard
   [re-cog.facts.datalog :refer (desktop?)]
   [taoensso.timbre :refer (refer-timbre)]
   [mount.core :as mount :refer (defstate)]
   [clara.rules :refer :all]
   [qbits.knit :refer (executor) :as knit]
   [re-core.queue :refer (process)]))

(refer-timbre)

(defn fact-type [fact]
  (or (:state fact) (:type fact)))

(defn initialize []
  (atom
   (mk-session
    're-flow.queries 're-flow.setup 're-flow.restore
    're-flow.certs 're-flow.notification 're-flow.disposable
    're-flow.nebula 're-flow.dashboard
    :fact-type-fn fact-type :cache false)))

(defstate ^{:on-reload :noop} session
  :start (initialize)
  :stop (reset! session nil))

(defn update- [facts]
  (reset! session (fire-rules (insert-all @session facts))))

(derive ::system :re-flow.session/type)

(defn populate-system-facts
  "Adding system information facts on initialize"
  []
  (info "adding system facts")
  (update-
   [{:type ::system :desktop (desktop?)}]))

(defn start-
  "A worker for processing fact results from async processes"
  []
  (let [e (executor :fixed {:num-threads 20})]
    (populate-system-facts)
    (info "starting facts processor")
    (knit/future
      (process
       (fn [facts]
         (info "updating facts" facts)
         (update- facts)
         {:facts facts})
       ::facts) {:executor e})
    e))

(defn stop- [e]
  (when e
    (.shutdown e)))

(defstate facts-updater
  :start (start-)
  :stop (stop- facts-updater))

(defn run-query [q]
  (query @session q))

(comment
  (update-
   [{:state :re-flow.setup/provisioned :flow :re-flow.restore/restore :failure false :timeout true}])
  (update-
   [{:state :re-flow.certs/spec :flow :re-flow.certs/certs :failure true :message "bla"}])
  (update-
   [{:state :re-flow.restore/partitioned :failure false}
    {:state :re-flow.restore/mounted :failure false}]))
