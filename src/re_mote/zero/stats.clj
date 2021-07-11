(ns re-mote.zero.stats
  "General machine stats"
  (:require
   re-mote.repl.base
   [re-cog.scripts.stats :refer (net-script cpu-script free-script load-script du-script entropy-script)]
   [re-cog.zero.scheduled :refer (get-scheduled-result)]
   [clojure.core.strint :refer (<<)]
   [clojure.string :refer (split split-lines)]
   [re-mote.zero.pipeline :refer (run-hosts schedule-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [com.rpl.specter :as s :refer (transform select MAP-VALS ALL pred ATOM keypath multi-path)]
   [clj-time.core :as t]
   [clj-time.coerce :refer (to-long)]
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-share.schedule :refer (watch seconds)])
  (:import re_mote.repl.base.Hosts))

(refer-timbre)

(defn space [line]
  (split line #"\s"))

(defn comma [line]
  (split line #","))

(defn zipped [parent k ks by {:keys [result] :as m}]
  (let [lines (split-lines (result :out))
        ms (mapv (fn [line] (zipmap ks (by line))) lines)]
    (assoc-in m [parent k]
              (if (> (count ms) 1) ms (first ms)))))

(defn zip
  "Collecting output into a hash, must be defined outside protocoal because of var args"
  [this [parent k & ks] {:keys [success failure] :as res}]
  (let [by (or (first (filter fn? ks)) space)
        success' (map (partial zipped parent k (filter keyword? ks) by) success)]
    [this (assoc (assoc res :success success') :failure failure)]))

(def readings (atom {}))

(defn safe-dec [v]
  (try
    (bigdec v)
    (catch Throwable e
      (error (<< "failed to convert {v} into big decimal") e))))

(def single-nav
  [:success ALL :stats MAP-VALS MAP-VALS])

(defn multi-nav [& ks]
  [:success ALL :stats MAP-VALS ALL (apply multi-path ks)])

(defn into-dec
  ([v]
   (into-dec single-nav v))
  ([nav [this readings]]
   [this (transform nav safe-dec readings)]))

(defn reset
  "reset a key in readings"
  [k]
  (transform [ATOM MAP-VALS MAP-VALS] (fn [m] (dissoc m k)) readings))

(defn select-
  "select a single key from readings"
  [k]
  (select [ATOM MAP-VALS MAP-VALS (keypath k)] readings))

(defn last-n
  "keep last n items of a sorted map"
  [n m]
  (let [v (vec (into (sorted-map) m)) c (count v)]
    (if (< c n) m (into (sorted-map) (subvec v (- c n) c)))))

(def timeout [5 :second])

(def scripts {:free free-script
              :net net-script
              :cpu cpu-script
              :entropy entropy-script
              :du du-script
              :load load-script})

(defprotocol Stats
  (du [this] [this m])
  (entropy [this] [this m])
  (net [this] [this m])
  (cpu [this] [this m])
  (free [this] [this m])
  (load-avg [this] [this m])
  (collect [this m])
  (sliding [this m f k])
  (schedule [this k n capacity]))

(defn split-results [{:keys [success] :as m}]
  (assoc m :success
         (flatten (map (fn [{:keys [result] :as r}]
                         (map (fn [{:keys [exit] :as v}] (assoc r :result v :code exit)) result)) success))))

(defn normalize [this m & ks]
  (->> m
       split-results
       (zip this ks)
       into-dec))

(extend-type Hosts
  Stats
  (du
    ([this]
     (into-dec
      (multi-nav :blocks :used :available)
      (zip this [:stats :du :filesystem :type :blocks :used :available :perc :mount]
           (split-results (run-hosts this get-scheduled-result [:du] timeout)))))
    ([this _]
     (du this)))

  (entropy
    ([this]
     (normalize this (run-hosts this get-scheduled-result [:entropy] timeout) :stats :entropy :available))
    ([this _]
     (entropy this)))

  (net
    ([this]
     (normalize this (run-hosts this get-scheduled-result [:net] timeout)
                :stats :net :rxpck/s :txpck/s :rxkB/s :txkB/s :rxcmp/s :txcmp/s :rxmcst/s :ifutil))
    ([this _]
     (net this)))

  (cpu
    ([this]
     (normalize this (run-hosts this get-scheduled-result [:cpu] timeout) :stats :cpu :usr :sys :idle))
    ([this _]
     (cpu this)))

  (free
    ([this]
     (normalize this (run-hosts this get-scheduled-result [:free] timeout) :stats :free :total :used :free :shared :buff-cache :available))
    ([this _]
     (free this)))

  (load-avg
    ([this]
     (normalize this (run-hosts this get-scheduled-result [:load-avg] timeout) :stats :load :one :five :fifteen :cores))
    ([this _]
     (load-avg this)))

  (collect [this {:keys [success] :as m}]
    (doseq [{:keys [host stats]} success]
      (doseq [[k v] stats]
        (swap! readings update-in [host k :timeseries]
               (fn [m] (if (nil? m) (sorted-map (t/now) v) (assoc m (t/now) v))))))
    [this m])

  (schedule
    ([this k n capacity]
     (schedule-hosts this shell (shell-args (scripts k) :cached? true) [k n capacity]))))

(defn purge [n]
  (transform [ATOM MAP-VALS MAP-VALS MAP-VALS] (partial last-n n) readings))

(defn refer-stats []
  (require '[re-mote.zero.stats :as stats :refer (load-avg net cpu free du entropy collect)]))

(comment
  (reset! readings {}))

