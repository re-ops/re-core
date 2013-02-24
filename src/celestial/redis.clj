(ns celestial.redis
  (:use  
    [celestial.common :only (config)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+]]
    [taoensso.timbre :only (debug trace info error warn)])
  (:require  
    [taoensso.carmine.message-queue :as carmine-mq]
    [taoensso.carmine :as car])
  (:import java.util.Date))

(def pool (car/make-conn-pool)) 

(def spec-server1 (car/make-conn-spec :host (get-in config [:redis :host])))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn curr-time [] (.getTime (Date.)))

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

(defn create-worker [name f]
  (carmine-mq/make-dequeue-worker pool spec-server1 name :handler-fn f))

(defn clear-all []
  (wcar (car/flushdb)))
