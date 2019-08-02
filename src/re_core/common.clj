(ns re-core.common
  (:import java.util.Date)
  (:refer-clojure :exclude [read-string])
  (:require
   pallet.stevedore.bash
   [clojure.core.strint :refer (<<)]
   [clojure.edn :refer (read-string)]
   [pallet.stevedore :refer  [script with-source-line-comments]]
   [taoensso.timbre :refer (refer-timbre)])
  (:import java.net.InetAddress))

(refer-timbre)

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defmacro bash- [& forms]
  `(with-source-line-comments false
     (script ~@forms)))

; basic time manipulation
(defn curr-time [] (.getTime (Date.)))

(def minute (* 1000 60))

(def half-hour (* minute 30))

(def hostname
  (.getHostName (InetAddress/getLocalHost)))

(defn slurp-edn [file]
  (read-string (slurp file)))

(defmacro print-e [f]
  `(try
     ~f
     (catch Exception e#
       (println e#))))
