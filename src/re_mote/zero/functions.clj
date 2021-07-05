(ns re-mote.zero.functions
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.send :refer (send-)]
   [re-share.core :refer (gen-uuid)]
   [serializable.fn :as s]))

(refer-timbre)

; Misc
(def ^{:doc "A liveliness ping"} ping
  (s/fn [] :ok))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns]))

(defn call
  "Launch a remote serialized functions on a list of hosts running re-gent"
  [f args zhs]
  {:pre [(not (nil? zhs))]}
  (let [uuid (gen-uuid)]
    (doseq [[_ address] zhs]
      (send- address {:request :execute :uuid uuid :fn f :args args}))
    uuid))

(defn schedule
  "Set up a scheduled serialized fn with args who result is persisted under k and runs every n seconds"
  [f args [k n] zhs]
  {:pre [(not (nil? zhs))]}
  (let [uuid (gen-uuid)]
    (doseq [[_ address] zhs]
      (send- address {:request :schedule :uuid uuid :fn f :k k :n n :args args}))
    uuid))

(comment
  (processes-named "ssh"))
