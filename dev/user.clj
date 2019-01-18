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
   [re-core.presets.digitial :refer (refer-digital-presets)]
   [re-core.presets.aws :refer (refer-aws-presets)]
   [re-core.presets.common :refer (refer-common-presets)]
   [re-core.presets.type :refer (refer-type-presets)]
   ; re-core components
   [mount.core :as mount]
   [re-core.queue :refer (queue)]
   [re-core.workers :refer (workers)]
   [re-core.schedule :refer (schedule)]
   [es.common :as core-es]
   ; utilities
   [es.history :refer (refer-history)]
   [re-core.repl.fixtures :refer :all]
   [me.raynes.fs :refer (extension file?)]
   ; logging
   [re-share.log :refer (redirect-output debug-on debug-off)]
   [re-core.log :refer (setup-logging)]
   ; Elasticsearch
   [rubber.core :refer :all :exclude (clear get create call)]
   ; re-mote
   [re-mote.repl :refer :all :exclude (provision)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.log :refer (log-hosts)]
   [re-mote.zero.stats :refer (disk-breach)]
   ; re-mote components
   [re-mote.zero.cycle :refer (zero)]
   [re-mote.persist.es :as mote-es :refer (elastic)]
   [re-share.config :as conf]
   [re-share.zero.keys :as k]
   [re-share.schedule :as sc]
   [re-mote.publish.riemann :refer (riemann)]
   ; testing
   [clojure.test])
  (:import re_mote.repl.base.Hosts))

(refer-history)
(refer-zero-manage)
(refer-kvm-presets)
(refer-aws-presets)
(refer-digital-presets)
(refer-common-presets)
(refer-type-presets)

(defn into-hosts [auth hosts]
  (Hosts. auth hosts))

(def system nil)

(re-core.model/set-dev)

(defn start-
  "Starts the current development system."
  []
  (setup-logging)
  (conf/load (fn [_] {}))
  (k/create-server-keys ".curve")
  (mount/start #'elastic #'zero #'schedule #'queue #'workers #'riemann))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (sc/halt!)
  (mount/stop))

(defn go
  "Initializes the current development system and starts it running."
  []
  (start-)
  (doseq [f (filter #(and (file? %) (= ".clj" (extension %))) (file-seq (io/file "scripts")))]
    (load-file (.getPath f))))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn require-tests []
  (require
   're-core.integration.es.jobs
   're-core.integration.es.systems
   're-core.test.aws
   're-core.test.kvm
   're-core.test.physical
   're-core.test.provider
   're-core.test.validations
   're-core.features.digitial
   're-core.features.kvm
   're-core.features.ec2))

(defn run-tests []
  (clojure.test/run-tests
   're-core.test.aws
   're-core.test.kvm
   're-core.test.physical
   're-core.test.provider
   're-core.test.validations))

(defn run-integration
  "run integration tests"
  []
  (clojure.test/run-tests
   're-core.integration.es.jobs
   're-core.integration.es.systems))

(defn run-provider
  "run provider tests"
  []
  (clojure.test/run-tests
   're-core.features.kvm
   ;; 're-core.features.ec2
   ;; 're-core.features.digital
))

(defn es-switch
  "Switch ES connection"
  [k {:keys [re-core] :as s}]
  (let [{:keys [es]} re-core]
    (.stop es)
    (prefix-switch k)
    (.setup es)
    (.start es)
    s))

(defn switch-
  "Starts the current development system."
  [k]
  (alter-var-root #'system (partial es-switch k)))

(defn history
  ([]
   (history identity))
  ([f]
   (doseq [line (filter f (clojure.string/split (slurp ".lein-repl-history") #"\n"))]
     (println line))))

(defn clrs
  "clean repl"
  []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))
