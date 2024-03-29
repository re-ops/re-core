(ns re-core.queue
  "Durable worker queues"
  (:require
   [re-core.common :refer (stack-trace)]
   [re-share.config.core :refer (get!)]
   [me.raynes.fs :refer (mkdir)]
   [re-core.persistency.jobs :as jobs]
   [taoensso.timbre :refer (refer-timbre)]
   [mount.core :as mount :refer (defstate)]
   [durable-queue :refer (take! put! complete! queues) :as dq]))

(refer-timbre)

(def q (atom nil))

(defn stats []
  (dq/stats @q))

(def run (atom true))

(defn- now [] (System/currentTimeMillis))

(defn- save-status
  "marks as succesful"
  [job topic status start f result]
  (let [job' (merge job (meta f) {:start start :end (now) :topic topic :status status :result result})]
    (jobs/put job')
    (trace "saved status" status job')))

(defn process [f topic]
  (while @run
    (let [task (take! @q (name topic) 1000 :timed-out!)]
      (when-not (= task :timed-out!)
        (let [{:keys [tid args] :as job} (deref task) start (now)]
          (try
            (debug "start processing " topic tid)
            (let [result (apply f args)]
              (save-status job topic :success start f result))
            (debug "done processing " topic tid)
            (catch Throwable e
              (error "queue process failed" (stack-trace e))
              (save-status (assoc job :message (.getMessage e)) topic :failure start f {}))
            (finally
              (complete! task)))))))
  (debug "worker for" topic "going down"))

(defn enqueue [topic job]
  (put! @q topic job))

(defn status [{:keys [tid]}]
  {:pre [(not (nil? tid))]}
  (when-let [job (jobs/get tid)]
    (-> job :status keyword)))

(defn- start- []
  (let [dir (get! :re-core :queue-dir)]
    (reset! run true)
    (mkdir dir)
    (reset! q (queues dir {:complete? (fn [_] true)}))
    (info "queue is up")))

(defn- stop- []
  (reset! run false)
  (reset! q nil))

(defstate ^{:on-reload :noop} queue
  :start (start-)
  :stop (stop-))

