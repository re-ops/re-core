(ns re-core.provider
  "Common logic for all providers"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.core.incubator :refer (dissoc-in)]
   [subs.core :refer (validation when-not-nil)]
   [re-mote.ssh.transport :refer (ssh-up?)]
   [re-core.model :refer (hypervisor)]
   [re-share.config.core :refer (get!)]
   [re-share.encryption :refer (decrypt encrypt encode decode)]
   [clojure.data.json :as json]
   [re-share.wait :refer (wait-for)]
   [clojure.core.strint :refer (<<)])
  (:import clojure.lang.ExceptionInfo))

(refer-timbre)

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn repeates [k v]
  (if (set? k) (interleave k (repeat (count k) v)) [k v]))

(defn mappings
  "Maps raw model keys to specific model keys,
  single key can fan out to multiple keys using a set"
  [res ms]
  (let [mapped ((key-select ms) res)]
    (merge
     (reduce (fn [r [k _]] (dissoc r k)) res ms)
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
      (apply hypervisor ks))))

(defn transform
  "specific model transformations"
  [res ts]
  (reduce
   (fn [res [k v]] (update-in res [k] v)) res ts))

(defn wait-for-ssh [address user timeout]
  {:pre [address user timeout]}
  (let [k (get! :shared :ssh :private-key-path)]
    (wait-for {:timeout timeout :sleep [2000 :ms]}
              (fn []
                (try
                  (ssh-up? {:host address :port 22 :user user :ssh-key k})
                  (catch Throwable e
                    (debug (<< "Failed to ssh to ~{address} due to ~(.getMessage e) with ~{user} ~{k}"))
                    false)))
              (<< "Timed out while waiting for ssh please check: ssh ~{user}@~{address} -i ~{k}"))))

(defn map-key [m from to]
  (dissoc-in (assoc-in m to (get-in m from)) from))

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

(defn into-mb
  "Convert RAM units to Megabytes"
  [units]
  (int (* 1024 units)))

(defn into-description
  "Create a description string from system, the string is base64 encoded and encrypted"
  [system]
  (encode
   (encrypt
    (json/write-str (dissoc system :system-id)) (get! :shared :pgp :public))))

(defn from-description
  "Convert description back into a system"
  [description]
  (json/read-json
   (decrypt (decode description) (get! :shared :pgp :private) (get! :shared :pgp :pass))))
