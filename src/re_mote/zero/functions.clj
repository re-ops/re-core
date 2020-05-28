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
      (send- address {:request :execute :uuid  uuid :fn f :args args}))
    uuid))

(comment
  (processes-named "ssh"))
