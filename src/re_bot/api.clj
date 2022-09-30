(ns re-bot.api
  "Consuming re-bot events over http"
  (:require
   [clojure.data.json :as json]
   [clojure.core.strint :refer (<<)]
   [org.httpkit.client :as http]))

(defn get-events [host port t]
  (json/read-str (:body @(http/get (<< "http://~{host}:~{port}/events/~{t}"))) :key-fn keyword))

(comment
  (def t (System/currentTimeMillis))
  (get-events "192.168.122.120" 9090 t))
