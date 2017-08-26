(ns user
  (:require
   [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars)]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   ; re-core
   [re-core.repl :refer :all]
   [re-core.log :refer (debug-on debug-off)]
   [re-core.repl.fixtures :refer :all]
   [me.raynes.fs :refer (extension file?)]
   [re-core.launch :as core]
   ; re-mote
   [re-mote.repl :refer :all :exclude (provision)]
   [re-mote.zero.base :refer (call)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.log :refer (log-hosts redirect-output)]
   [re-mote.zero.functions :refer (plus-one ls)]
   [re-mote.launch :as mote]))

(refer-zero-manage)

(def system nil)

(re-core.model/set-dev)

(defn setup-all []
  (mote/setup)
  (core/setup))

(defn start-all [s]
  (mote/start nil)
  (core/start s))

(defn stop-all [s]
  (mote/stop nil)
  (core/stop s))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (setup-all))))

(defn start-
  "Starts the current development system."
  []
  (alter-var-root #'system start-all))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system (fn [s] (when s (stop-all s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start-)
  (doseq [f (filter #(and (file? %) (= ".clj" (extension %))) (file-seq (io/file "scripts")))]
    (load-file (.getPath f))))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn clrs
  "clean repl"
  []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))

(defn history []
  (println  (slurp ".lein-repl-history")))
