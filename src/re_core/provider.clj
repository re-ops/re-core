(ns re-core.provider
  "common providers functions"
  (:require
   [flatland.useful.map :refer (dissoc-in*)]
   [subs.core :refer (validation when-not-nil)]
   [re-mote.ssh.transport :refer (ssh-up?)]
   [re-core.model :refer (hypervisor)]
   [minderbinder.time :refer (parse-time-unit)]
   [re-core.common :refer (get! curr-time)]
   [re-share.core :refer (wait-for)]
   [clojure.core.strint :refer (<<)])
  (:import clojure.lang.ExceptionInfo))

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
      (try
        (apply hypervisor ks)
        (catch ExceptionInfo e
          (when (= (-> ex-data :type) :re-core.common/missing-conf)
            (throw
             (ex-info (<< "no matching template found for ~{os} add one to configuration under ~{ks}") {:ks ks}))))))))

(defn transform
  "specific model transformations"
  [res ts]
  (reduce
   (fn [res [k v]] (update-in res [k] v)) res ts))

(defn wait-for-ssh [address user timeout]
  {:pre [address user timeout]}
  (let [k (get! :ssh :private-key-path)]
    (wait-for {:timeout timeout}
              (fn []
                (try
                  (ssh-up? {:host address :port 22 :user user :ssh-key k})
                  (catch Throwable e false)))
              (<< "Timed out while waiting for ssh please check: ssh ~{user}@~{address} -i ~{k}"))))

(defn map-key [m from to]
  (dissoc-in* (assoc-in m to (get-in m from)) from))

; common validations
(validation :ip (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+") "must be a legal ip address"))

(validation :mac (when-not-nil (partial re-find #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") "must be a legal mac address"))

(validation :device #(when-not (re-find (re-matcher #"\/dev\/\w+" %)) "device should match /dev/{id} format"))

(test #'mappings)

(defn running? [this] (= (.status this) "running"))

(defn wait-for-stop
  "Wait instance to stop"
  [this timeout]
  (wait-for {:timeout timeout} #(not (running? this))
            "Timed out waiting for stop"))

(defn wait-for-start
  "Wait for an ip to be avilable"
  [this timeout ex]
  (wait-for {:timeout timeout} #(running? this)
            "Timed out waiting for instance to be running"))
