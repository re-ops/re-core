(ns re-flow.common
  "common flow functions"
  (:require
   [re-share.core :refer (gen-uuid)]
   [re-core.queue :refer [enqueue]]
   [re-core.repl :refer (hosts with-ids)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.repl :refer (named) :as repl]
   [com.rpl.specter :refer [select ALL keypath]]))

(refer-timbre)

(defn successful-systems
  "Get the successful system ids from a Re-core pipeline result"
  [f]
  (select [ALL (keypath :results :success) ALL :args ALL :system-id] @f))

(defn successful-hosts
  "Get the successful hots ids from a Re-mote pipeline result"
  [hs]
  (select [ALL (keypath :success) ALL :host] hs))

(defn successful-ids [hs]
  (let [systems (repl/list (named (successful-hosts hs)) :systems :print? false)]
    (into #{} (->> systems second :systems (map first)))))

(defn run-?e
  "Run Re-mote pipeline on system ids provided by ?e and check if all were successful"
  [f {:keys [ids] :as ?e} & args]
  (let [result (apply (partial f (hosts (with-ids ids) :hostname)) args)]
    (= (into #{} ids) (successful-ids result))))

(defn fact-callback [fact pred]
  (fn [timeout m]
    (enqueue :re-flow.session/facts {:tid (gen-uuid) :args [[{:state fact :timeout timeout :failure (pred m)}]]})))

(defn run-?e-non-block
  "Run a Hosts function on system ids provided by ?e without blocking for the result.
   Once the run is done the provided fact will be inserted with :failure set according to provided predicate."
  [f {:keys [ids] :as ?e} fact timeout pred & args]
  (apply (partial f (hosts (with-ids ids) :hostname)) (concat args [timeout (fact-callback fact pred)])))

(comment
  (enqueue :re-flow.session/facts {:tid "1234" :args [[{:state :re-flow.restore/restored :timeout true :failure false}]]}))
