(ns celestial.redis
  "Redis utilities like a distributed lock, connection managment and ref watcher"
  (:use  
    [celestial.common :only (config curr-time)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+]]
    [taoensso.timbre :only (debug trace info error warn)])
  (:require  
    [taoensso.nippy :as nippy]
    [taoensso.carmine.message-queue :as carmine-mq]
    [taoensso.carmine :as car])
  (:import java.util.Date))

(def pool (car/make-conn-pool)) 

(def spec-server (car/make-conn-spec :host (get-in config [:redis :host])))

(def wcar-disable false)

(defmacro wcar [& body] 
  `(if-not wcar-disable 
     (car/with-conn pool spec-server ~@body)
     ~@body
     ))


(defn lock-id [k] (<< "lock.~{k}"))

(defn- gen-uuid [] (str (java.util.UUID/randomUUID)))

(defn- expire [lid expiry]
  (wcar (car/expire lid expiry)))

(defn acquire 
  "Acquires lock, returns uuid if successful 
   wait-time (mili) wait time for lock,
   expiry (sec) how long the lock is valid 
   see http://dr-josiah.blogspot.co.il/2012/01/creating-lock-with-redis.html "
  [id & [{:keys [wait-time expiry] :or {wait-time 2000 expiry 2} :as opts} & _]]
  {:pre [(or (nil? opts) (associative? opts))]}
  (let [lid (lock-id id) uuid (gen-uuid)
        wait (+ (curr-time) wait-time)]
    (loop []
      (if (> wait (curr-time))
        (if (= 1 (wcar (car/setnx lid uuid)))
          (do (expire lid expiry) uuid)
          (do 
            (when (< (wcar (car/ttl lid)) 0)
              (expire lid expiry))
            (recur))) 
        false)))) 

(defn get- [k]
  (wcar (car/get k)))

(defn release [id uuid] 
  (let [lid (lock-id id)]
    (wcar (car/watch lid)) 
    (if (= (get- lid)  uuid)
      (do (wcar (car/del lid)) true)
      (do (wcar (car/unwatch)) false))))

(defn with-lock 
  "Try to to obtain lock for id and execute f, throw exception if fails"
  [id f & [opts]]
  (trace "attempting to aquire lock on" id opts)
  (if-let [uuid (acquire id opts)]
    (try
      (trace "lock acquired" id)
      (f) 
      (finally 
        (release id uuid)
        (trace "lock released" id)
        ))
    (throw+ {:type ::lock-fail :id id} "Failed to obtain lock")))

(def minute (* 1000 60))

(def half-hour (* minute 30))

(defn atom-key [k] (str "atom" k))

(defn apply-diff [map-key _new old]
  {:pre [(map? _new) (map? old)]}
  (fn [] 
    (letfn [(sub [a b] (clojure.set/difference (into #{} a) (into #{} b)))]
      (doseq [[k v] (sub old _new)] (wcar (car/hdel map-key k)))
      (doseq [[k v] (sub _new old)] (wcar (car/hset map-key k (car/preserve v))))
      )))

(defn sync-watch [map-key r]
  (add-watch r map-key
     (fn [_key _ref old _new] 
        (with-lock  _key (apply-diff map-key _new old)
                 {:expiry half-hour :wait-time minute}))) r)

(defn synched-map [k]
  "Lock backed atom map watcher that persists changes into redis takes the backing redis hash key."
  (sync-watch (atom-key k)
    (if-let [data (wcar (car/hgetall* (atom-key k)))]
       (atom data)  
       (atom {}))))

(defn create-worker [name f]
  (carmine-mq/make-dequeue-worker pool spec-server name :handler-fn f))

(defn clear-all []
  (wcar (car/flushdb)))
