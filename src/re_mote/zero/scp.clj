(ns re-mote.zero.scp
  (:require
   re-mote.repl.base
   [re-cog.scripts.common :refer (shell-args shell)]
   [re-cog.scripts.scp :as s]
   [re-mote.zero.pipeline :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Scping
  (scp-from [this target hs-src src recursive?]))

(defn merge-results [acc {:keys [success failure hosts]}]
  (-> acc
      (clojure.core/update :hosts (partial into hosts))
      (clojure.core/update :success (partial into success))
      (clojure.core/update :failure (partial merge-with conj failure))))

(extend-type Hosts
  Scping
  (scp-from [this target hs-src src recursive?]
    (let [{:keys [auth hosts]} hs-src
          {:keys [user]} auth
          results (map #(run-hosts this shell (shell-args (s/scp-from user % src target recursive?)) [360 :second]) hosts)]
      [this (reduce merge-results {} results)])))

(defn refer-zero-scp []
  (require '[re-mote.zero.scp :as z-scp]))
