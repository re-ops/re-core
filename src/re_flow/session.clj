(ns re-flow.session
  "Clara session managment"
  (:require
   re-flow.setup
   re-flow.queries
   re-flow.restore
   [taoensso.timbre :refer (refer-timbre)]
   [mount.core :as mount :refer (defstate)]
   [clara.rules :refer :all]
   [qbits.knit :refer (executor) :as knit]
   [re-core.queue :refer (process)]))

(refer-timbre)

(defn initialize []
  (atom (mk-session 're-flow.queries 're-flow.setup 're-flow.restore :fact-type-fn :state :cache false)))

(def session (initialize))

(defn update- [facts]
  (let [new-facts (reduce (fn [s fact] (insert s fact)) @session facts)]
    (reset! session (fire-rules new-facts))))

(defn start- []
  (let [e (executor :fixed {:num-threads 20})]
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
   [{:state :re-flow.restore/restored :failure false :timeout false}])
  (update-
   [{:state :re-flow.restore/partitioned :failure false}
    {:state :re-flow.restore/mounted :failure false}]))
