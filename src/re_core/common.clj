(ns re-core.common
  (:import java.util.Date)
  (:require
   pallet.stevedore.bash
   [clojure.core.strint :refer (<<)]
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

(defn gen-uuid []
  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(def version "0.6.0")

(defn resolve-
  "resolve function provided as a symbol with the form of ns/fn"
  [fqn-fn]
  (let [[n f] (.split (str fqn-fn) "/")]
    (try
      (require (symbol n))
      (ns-resolve (find-ns (symbol n)) (symbol f))
      (catch java.io.FileNotFoundException e
        (throw (ex-info (<< "Could not locate ~{fqn-fn}") {:fn fqn-fn}))))))

(def hostname
  (.getHostName (InetAddress/getLocalHost)))

(defn slurp-edn [file]
  (read-string  (slurp file)))
