(ns re-core.schedule
  "Scheduled tasks"
  (:require
   [mount.core :as mount :refer (defstate)]
   [re-core.common :refer (resolve-)]
   [re-share.config :refer (get*)]
   [taoensso.timbre :refer (refer-timbre)]
   [chime :refer [chime-ch]]
   [clj-time.core :as t]
   [clj-time.periodic :refer  [periodic-seq]]
   [clojure.core.async :as a :refer [<! close! go-loop]]))

(refer-timbre)

(defn schedule [f chimes args]
  (go-loop []
    (if-let [msg (<! chimes)]
      (do (f args) (recur))
      (info f "scheduled task stopped"))))

(defn time-fn [unit]
  (resolve-  (symbol (str "clj-time.core/" (name unit)))))

(defn schedules []
  (map
   (fn [[f {:keys [every args]}]]
     (let [[t unit] every]
       [(resolve- f) (chime-ch (periodic-seq (t/now) ((time-fn unit) t))) args])) (get* :scheduled)))

(defn close-and-flush
  "See https://groups.google.com/forum/#!topic/clojure-dev/HLWrb57JIjs"
  [c]
  (close! c)
  (clojure.core.async/reduce (fn [_ _] nil) [] c))

(defn start [scs]
  (info "Starting scheduled tasks")
  (doseq [s scs]
    (apply schedule s))
  scs)

(defn stop [scs]
  (doseq [[_ c _] scs] (close-and-flush c)))

(defstate scehdule
  :start (start (schedules))
  :stop (stop scehdule))
