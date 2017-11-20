(ns re-core.provider
  "common providers functions"
  (:require
   [flatland.useful.map :refer (dissoc-in*)]
   [subs.core :refer (validation when-not-nil)]
   [re-mote.ssh.transport :refer (ssh-up?)]
   [re-core.model :refer (hypervisor)]
   [minderbinder.time :refer (parse-time-unit)]
   [re-core.common :refer (get! curr-time)]
   [clojure.core.strint :refer (<<)]
   [slingshot.slingshot :refer (throw+ try+)]))

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn repeates [k v]
  (if (set? k) (interleave k (repeat (count k) v)) [k v]))

(defn mappings
  "Maps raw model keys to specific model keys,
  single key can fan out to multiple keys using a set"
  [res ms]
  (let [mapped ((key-select ms) res)]
    (merge
     (reduce (fn [r [k v]] (dissoc r k)) res ms)
     (reduce (fn [r [k v]] (apply assoc r (repeates (ms k) v))) {} mapped))))

(defn selections
  "Select group of keys from map"
  ([m kys] ((selections kys) m))
  ([kys]
   (letfn [(select [k] (fn [m] (select-keys m k)))]
     (apply juxt (map select kys)))))

(defn os->template
  "Os key to template/VM/image"
  [hyp]
  (fn [os]
    (let [ks [hyp :ostemplates os]]
      (try+
       (apply hypervisor ks)
       (catch [:type :re-core.common/missing-conf] e
         (throw+ {:type :missing-template}
                 (<< "no matching template found for ~{os} add one to configuration under ~{ks}")))))))

(defn transform
  "specific model transformations"
  [res ts]
  (reduce
   (fn [res [k v]] (update-in res [k] v)) res ts))

(defn wait-for
  "A general wait for pred function"
  [{:keys [timeout sleep] :or {sleep [1 :seconds]} :as timings} pred err message]
  {:pre [(map? timings)]}
  (let [wait (+ (curr-time) (parse-time-unit timeout))]
    (loop []
      (if (> wait (curr-time))
        (if (pred)
          true
          (do (Thread/sleep (parse-time-unit sleep)) (recur)))
        (throw+ (merge err timings) message)))))

(defn wait-for-ssh [address user timeout]
  {:pre [address user timeout]}
  (wait-for {:timeout timeout}
            #(try
               (ssh-up? {:host address :port 22 :user user})
               (catch Throwable e false))
            {:type ::ssh-failed :timeout timeout} "Timed out while waiting for ssh"))

(defn map-key [m from to]
  (dissoc-in* (assoc-in m to (get-in m from)) from))

; common validations
(validation :ip (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+") "must be a legal ip address"))

(validation :mac (when-not-nil (partial re-find #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") "must be a legal mac address"))

(validation :device #(when-not (re-find (re-matcher #"\/dev\/\w+" %)) "device should match /dev/{id} format"))

(test #'mappings)

(defn running? [this] (= (.status this) "running"))

(defn wait-for-stop
  "Wait for an ip to be avilable"
  [this timeout ex]
  (wait-for {:timeout timeout} #(not (running? this))
            {:type ex :timeout timeout}
            "Timed out on waiting for ip to be available"))

(defn wait-for-start
  "Wait for an ip to be avilable"
  [this timeout ex]
  (wait-for {:timeout timeout} #(running? this)
            {:type ex :timeout timeout}
            "Timed out on waiting for ip to be available"))
