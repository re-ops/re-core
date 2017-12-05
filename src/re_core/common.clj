(ns re-core.common
  (:import java.util.Date)
  (:require
   pallet.stevedore.bash
   [pallet.stevedore :refer  [script with-source-line-comments]]
   [taoensso.timbre :refer (refer-timbre)])
  (:use
   [slingshot.slingshot :only  [throw+ try+]]
   [re-core.config :only (config)]
   [clojure.core.strint :only (<<)]))

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defmacro bash- [& forms]
  `(with-source-line-comments false
     (script ~@forms)))

(refer-timbre)

(defn get!
  "Reading a keys path from configuration raises an error of keys not found"
  [& keys]
  (if-let [v (get-in config keys)]
    v
    (throw+ {:type ::missing-conf} (<< "No matching configuration keys ~{keys} found"))))

(defn get*
  "nil on missing version of get!"
  [& keys]
  (get-in config keys))

(defn envs
  "get all currently defined env keys"
  []
  (keys (get* :hypervisor)))

(defn slurp-edn [file] (read-string (slurp file)))

; basic time manipulation
(defn curr-time [] (.getTime (Date.)))

(def minute (* 1000 60))

(def half-hour (* minute 30))

(defn gen-uuid [] (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(defn interpulate
  "basic string interpulation"
  [text m]
  (clojure.string/replace text #"~\{\w+\}"
                          (fn [^String groups] ((keyword (subs groups 2 (dec (.length groups)))) m))))

(def version "0.13.5")

(defn resolve-
  "resolve function provided as a symbol with the form of ns/fn"
  [fqn-fn]
  (let [[n f] (.split (str fqn-fn) "/")]
    (try+
     (require (symbol n))
     (ns-resolve (find-ns (symbol n)) (symbol f))
     (catch java.io.FileNotFoundException e
       (throw+ {:type ::hook-missing} (<< "Could not locate hook ~{fqn-fn}"))))))
