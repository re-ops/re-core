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
   re-flow.react
   re-flow.view
   re-view.registry
   [re-cog.facts.datalog :refer (desktop?)]
   [taoensso.timbre :refer (refer-timbre)]
   [mount.core :as mount :refer (defstate)]
   [clara.rules :refer :all]
   [qbits.knit :refer (executor) :as knit]
   [re-core.queue :refer (process)]))

(refer-timbre)

(defn check-type [{:keys [state type]}]
  (cond
    (isa? state :re-flow.core/state) true
    (isa? type :re-flow.session/type) true
    :else (do (error "fact state/type doesn't match any valid option!" state type) false)))

(defn fact-type [fact]
  {:pre [(check-type fact)]}
  (or (:state fact) (:type fact)))

(defn populate-system-facts
  "Adding system information facts on initialize"
  [s]
  (info "adding system facts")
  (fire-rules (insert-all s [{:type ::system :desktop (desktop?)}
                             {:type ::system :smtp (not (nil? (re-share.config.core/get* :shared :smtp)))}])))

(defn create-session []
  (populate-system-facts
   (mk-session
    're-flow.queries 're-flow.setup 're-flow.restore
    're-flow.certs 're-flow.notification 're-flow.disposable
    're-flow.nebula 're-flow.dashboard 're-flow.react
    're-flow.view 're-view.registry
    :fact-type-fn fact-type :cache false)))

(defstate ^{:on-reload :noop} session
  :start (atom (create-session))
  :stop (reset! session nil))

(defn update- [facts]
  (reset! session (fire-rules (insert-all @session facts))))

(derive ::system :re-flow.session/type)

(defn create-update-workers
  "A worker pool for processing fact results from async processes"
  []
  (let [e (executor :fixed {:num-threads 20})]
    (knit/future
      (process
       (fn [facts]
         (debug "updating facts" facts)
         (update- facts)
         {:facts facts})
       ::facts) {:executor e})
    (info "re-flow facts processor is ready")
    e))

(defn halt-executor [e]
  (when e
    (.shutdownNow e)
    (try
      (.awaitTermination e 1000 java.util.concurrent.TimeUnit/NANOSECONDS)
      (info "Fact update executor pool has been shutdown")
      (catch java.lang.InterruptedException e
        (error e)))))

(defstate fact-update-workers
  :start (create-update-workers)
  :stop (halt-executor fact-update-workers))

(defn run-query [q]
  (query @session q))

(comment
  (update-
   [{:state :re-flow.notification/notify :failure true :timeout true}]))
