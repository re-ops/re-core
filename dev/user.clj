(ns user
  (:refer-clojure :exclude  [update list])
  (:require
   [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars)]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   ; re-core
   [re-core.repl :refer :all]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.aws :refer (refer-aws-presets)]
   [re-core.presets.common :refer (refer-common-presets)]
   [re-core.presets.type :refer (refer-type-presets)]
   [es.history :refer (refer-history)]
   [re-core.repl.fixtures :refer :all]
   [me.raynes.fs :refer (extension file?)]
   [re-core.launch :as core]
   ; re-share
   [re-share.log :refer (redirect-output debug-on debug-off)]
   ; re-mote
   [re-mote.repl :refer :all :exclude (provision)]
   [re-mote.zero.functions :refer (call)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.log :refer (log-hosts)]
   [re-mote.zero.functions :refer (plus-one listdir)]
   [re-mote.zero.stats :refer (disk-breach)]
   [re-mote.launch :as mote]))

(refer-history)
(refer-zero-manage)
(refer-kvm-presets)
(refer-aws-presets)
(refer-common-presets)
(refer-type-presets)

(def system nil)

(re-core.model/set-dev)

(defn setup-all []
  {:re-mote (mote/setup) :re-core (core/setup)})

(defn start-all [{:keys [re-core re-mote] :as m}]
  (mote/start re-mote)
  (core/start re-core)
  m)

(defn stop-all [{:keys [re-core re-mote] :as m}]
  (mote/stop re-mote)
  (core/stop re-core)
  m)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (setup-all))))

(defn start-
  "Starts the current development system."
  []
  (alter-var-root #'system start-all))

(defn es-switch
  "Switch ES connection"
  [k {:keys [es] :as s}]
  (.stop es)
  (re-share.es.common/prefix-switch k)
  (.setup es)
  (.start es)
  s)

(defn switch-
  "Starts the current development system."
  [k]
  (alter-var-root #'system (partial es-switch k)))

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

(defn contains
  "return true if string contains substring"
  [sub]
  (fn [s]
    (.contains s sub)))

(defn history
  ([]
   (history identity))
  ([f]
   (doseq [line (filter f (clojure.string/split (slurp ".lein-repl-history") #"\n"))]
     (println line))))

