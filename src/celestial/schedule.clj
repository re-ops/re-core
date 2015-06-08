(ns celestial.schedule
  "Scheduled tasks"
 (:require 
   [celestial.common :refer (get* resolve- import-logging)]
   [chime :refer [chime-ch]]
   [components.core :refer (Lifecyle)]
   [clj-time.core :as t]
   [clj-time.periodic :refer  [periodic-seq]]
   [clojure.core.async :as a :refer [<! <!! go-loop]]))  

(import-logging)

(defn schedule [f t args]
  (let [chimes (chime-ch t)]
    (<!! 
      (go-loop []
        (when-let [msg (<! chimes)]
         (f args)
         (recur))))))

(defn time-fn [unit]
  (resolve-  (symbol (str "clj-time.core/" (name unit)))))

(defn load-schedules 
  "Load all scheduled tasks"
   []
  (doseq [[f m] (get* :scheduled) :let [{:keys [every args]} m]]
    (let [[t unit] every]
      (schedule (resolve- f) (periodic-seq (t/now) ((time-fn unit) t)) args))))

(defrecord Schedule
  [] 
  Lifecyle
  (setup [this])
  (start [this]
    (info "Starting scheduled tasks")
    (load-schedules))
  (stop [this]))

(defn instance [] (Schedule.))
