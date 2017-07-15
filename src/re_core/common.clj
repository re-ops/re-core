(ns re-core.common
  (:import java.util.Date)
  (:require
     pallet.stevedore.bash
    [pallet.stevedore :refer  [script with-source-line-comments]]
    [taoensso.timbre :refer (refer-timbre)]
    )
  (:use
    [slingshot.slingshot :only  [throw+ try+]]
    [re-core.config :only (config)]
    [clojure.core.strint :only (<<)]
    ))

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

(def query {
   :kibana3
      "query=tid:~{tid}&fields=@timestamp,message,tid,"
   :kibana4
      "query:(query_string:(analyze_wildcard:!t,query:'tid:~{tid}')),columns:!(message)"
   }
  )

(defn link
  "Returns a link for a given query and args matching current central logging system"
  [args]
  (when-let [{:keys [host type port]} (get* :re-core :log :gelf)]
    (case type
     :kibana3
       (<< "http://~{host}:~{port}/index.html#/dashboard/script/logstash.js?~(interpulate (query type) args)")
     :kibana4
       (<< "http://~{host}:~{port}/#/discover?_g=(time:(from:now-24h,mode:quick,to:now))&_a=(~(interpulate (query type) args),index:'logstash-*',sort:!('@timestamp',desc))")
     :gralog2 "TBD"
     :logstash "NaN"
     (warn (<< "no matching link found for ~{type}")))))

(defmacro wrap-errors
   "Wraps validation error responses in the API layer"
   [& body]
  `(try+ ~@body
    (catch (map? ~'%) e#
      (info e#)
      (bad-req (select-keys ~'&throw-context [:message :object])))
    (catch Object e#
      (error e#)
      (bad-req {:message "Error happend, please contact admin"}))
   )
  )

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
