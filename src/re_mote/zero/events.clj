(ns re-mote.zero.events
  (:require
   [re-share.schedule :refer [watch seconds]]
   [re-share.core :refer (gen-uuid)]
   [re-core.queue :refer [enqueue]]
   [re-mote.zero.results :refer (missing-results)]
   [re-mote.zero.management :refer (all-hosts unregister)]
   [re-mote.zero.functions :refer (ping call)]
   [taoensso.timbre :refer  (refer-timbre)]))

(refer-timbre)

(defn take-host-down [host]
  (info host "went down!")
  (when-let [address ((all-hosts) host)]
    (unregister address)
    (enqueue :re-flow.session/facts {:tid (gen-uuid) :args [[{:hostname host :state :re-flow.react/down}]]})))

(def ^{:doc "hosts to missing counts"} misses (atom {}))

(defn ping-check []
  (let [hosts (all-hosts)
        uuid (call ping [] hosts)
        hosts-ks (into #{} (keys hosts))]
    (Thread/sleep 1000)
    (let [absentees (missing-results hosts-ks uuid)]
      (doseq [absent absentees]
        (swap! misses update absent (fnil inc 0)))
      (doseq [present (clojure.set/difference hosts-ks absentees)]
        (swap! misses dissoc present)))))

(defn handle [e-type event]
  (trace e-type (bean event))
  (when (= e-type :disconnected)))

(def threshold 3)

(defn cleanup []
  (ping-check)
  (doseq [[missing-host _] (filter (fn [[h c]] (> c threshold)) @misses)]
    (take-host-down missing-host)
    (swap! misses dissoc missing-host)))

(defn watch-misses
  "Track misses and take hosts with multiple misses down"
  []
  (watch :track-zero-connectivity (seconds 1) cleanup))
