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

(defn lval [s] (Long/valueOf s))

(defn try-aquire [lid timeout]
  (let [ts (wcar (car/getset lid (timestamp timeout)))]
    (if (<  (lval ts) (curr-time)) true false )))

(defn- check-validity [lid timeout]
  "A lock already exists we check if it had a valid timestamp"
  (let [ts (wcar (car/get lid))]
    (if (< (lval ts) (curr-time)); its not valid
      (try-aquire lid timeout) false )))

(defn lock [k timeout]
  "see http://redis.io/commands/setnx"
  (let [lid (lock-id k)  res (wcar (car/setnx lid (timestamp timeout)))]
    (if (= res 1) 
      true
      (check-validity lid timeout)
      ))) 

(defn release [k] (car/del (lock-id k)))

;(lock 5 (* 1000 60))
;(wcar (car/get (lock-id 5)))
