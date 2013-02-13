(ns celestial.redis
  (:use  
    clojure.core.strint
    [taoensso.timbre :only (debug info error warn)])
  (:require  
    [taoensso.carmine :as car])
  (:import java.util.Date) 
  )

(def pool (car/make-conn-pool)) 

; TODO move to config file
(def spec-server1 (car/make-conn-spec :host "localhost"))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn curr-time [] (.getTime (Date.)))

(defn timestamp [timeout] (+ (curr-time) timeout 1))

(defn lock-id [k] (<< "lock.~{k}"))

#_(defn lval [s] (Long/valueOf s))

#_(defn expired? [ts]
   (<  (lval ts) (curr-time)))

#_(defn try-aquire [lid timeout]
  (let [ts (wcar (car/getset lid (timestamp timeout)))]
    (if (expired? ts)  true false)))

#_(defn- check-expired [lid timeout]
  "A lock already exists we check if it had a valid timestamp"
    (if (expired? (wcar (car/get lid)))
      (try-aquire lid timeout) false ))

#_(defn acquire_lock  [k timeout]
  "Locks a key k with expiry of timeout (mili), return true if lock successful
   see http://redis.io/commands/setnx"
  (let [lid (lock-id k)  res (wcar (car/setnx lid (timestamp timeout)))]
    (if (= res 1) 
      true
      (check-expired lid timeout)
      ))) 

(defn- gen-uuid [] (str (java.util.UUID/randomUUID)))

(defn acquire [id & [{:keys [wait-time expiry] :or {wait-time 2000 expiry 2}} & _]]
  "Acquires lock, returns lock unique id if successful 
   wait-time (mili) wait time for lock,
   expiry (sec) how long the lock is valid 
   see http://dr-josiah.blogspot.co.il/2012/01/creating-lock-with-redis.html "
  (let [lid (lock-id id) uuid (gen-uuid)
        wait (+ (curr-time) wait-time)]
    (loop []
      (if (< wait (curr-time))
        false
        (if (= 1 (wcar (car/setnx lid uuid)))
          (do (wcar (car/expire lid expiry)) uuid)
          (do 
            (when (< (wcar (car/ttl lid)) 0)
              (wcar (car/expire lid expiry)))
            (recur))))))) 


(defn release [id uuid] 
  (let [lid (lock-id id)]
    (wcar (car/watch lid)) 
    (if (= (wcar (car/get lid)) uuid)
      (do (wcar (car/del lid)) true)
      (do (wcar (car/unwatch)) false))))

;(wcar (car/get (lock-id 5)))
