(ns re-core.redis
  "Redis utilities like a distributed lock, connection managment and ref watcher"
  (:use
    [clojure.set :only (difference)]
    [flatland.useful.utils :only (defm)]
    [re-core.common :only (get! curr-time gen-uuid half-hour minute)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+]]
    [taoensso.carmine.locks :only (with-lock)]
    [taoensso.timbre :only (debug trace info error warn)])
  (:require
    [safely.core :refer [safely]]
    [taoensso.carmine.message-queue :as carmine-mq]
    [taoensso.carmine :as car])
  (:import java.util.Date))

(defn server-conn [] {:pool {} :spec (get! :redis)})

(defmacro wcar [& body]
    `(safely
       (car/wcar (server-conn) ~@body)
       :on-error
       :log-errors false
       :max-retry 5
       :message "Error while trying to connect to redis"
       :retry-delay [:random-range :min 2000 :max 5000]))

(defn get- [k] (wcar (car/get k)))

; TODO this should be enabled in upstream
(def ^:private lkey (partial car/kname "carmine" "lock"))

(defn clear-locks []
  (info "Clearing locks")
  (when-let [lkeys (seq (wcar (car/keys (lkey "*"))))]
    (wcar (apply car/del lkeys))))

(defn create-worker [name f]
  (carmine-mq/worker (server-conn) name {:handler f :eoq-backoff-ms 200}))

(defn clear-all []
  (info "Clearing redis db" (server-conn))
  (wcar (car/flushdb))
  )

