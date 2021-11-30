(ns user
  (:refer-clojure :exclude  [update list])
  (:require
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   ; Re-flow
   [re-flow.core :refer (trigger)]
   [re-flow.demo :refer :all]
   [re-flow.common :refer (create-fact)]
   [re-flow.session :refer (session fact-update-workers run-query)]
   [re-flow.queries :refer :all]
   [re-flow.pubsub :refer (rules-pubsub)]
   [re-flow.file-watcher :refer (watchers)]
   ; Re-core
   [re-core.repl.results :refer (refer-results)]
   [re-core.repl :refer :all]
   [re-core.dispoable :refer :all]
   [re-core.clipboard :refer (read-clipboard set-clipboard)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.digital :refer (refer-digital-presets)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.types :refer (refer-type-presets)]
   ; profiles
   [re-cipes.profiles]
   ; Re-core components
   [mount.core :as mount]
   [re-core.queue :refer (queue)]
   [re-core.workers :refer (workers)]
   ; Utilities
   [re-core.repl.fixtures :refer :all]
   [me.raynes.fs :refer (extension file?)]
   ; Logging
   [re-share.log :refer (redirect-output debug-on debug-off)]
   [re-core.log :refer (setup-logging disable-coloring)]
   ; Re-mote
   [re-mote.repl :refer :all :exclude (provision)]
   [re-mote.repl.base :refer (sync-)]
   [re-mote.repl.stress :refer (refer-stress)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.pipeline :refer (refer-zero-pipe)]
   ; Re-mote components
   [re-mote.zero.cycle :refer (zero)]
   ; Metrics persistency
   [re-mote.persist.es :as mote-es :refer (elastic)]
   ; System persistency
   [re-core.persistency.xtdb :as xtdb]
   [re-ops.config.core :as conf]
   [re-share.config.secret :refer (load-secrets)]
   [re-share.zero.keys :as k]
   [re-share.schedule :as sc]
   [re-mote.publish.riemann :refer (riemann)]
   ; Testing
   [clojure.test])
  (:import re_mote.repl.base.Hosts))

(refer-zero-manage)
(refer-zero-pipe)
(refer-kvm-presets)
(refer-digital-presets)
(refer-system-presets)
(refer-instance-types)
(refer-type-presets)
(refer-stress)
(refer-results)

(defn into-hosts [auth hosts]
  (Hosts. auth hosts))

(def system nil)

(re-core.model/set-dev)

(defn start-
  "Starts the current development system."
  []
  (load-secrets "secrets" "/tmp/secrets.edn" "keys/secret.gpg")
  (conf/load-config)
  (setup-logging)
  (disable-coloring)
  (k/create-server-keys ".curve")
  (mount/start #'queue #'xtdb/node #'session #'fact-update-workers #'rules-pubsub  #'workers #'zero #'watchers))

(defn start-metrics []
  (mount/start #'elastic #'riemann)
  (mote-es/initialize))

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
   're-flow.integration.certs
   're-core.integration.es.jobs
   're-core.integration.es.systems
   're-core.test.kvm
   're-core.test.physical
   're-core.test.provider
   're-core.test.validations
   're-core.features.digitial
   're-core.test.specs
   're-core.features.kvm
   're-core.features.lxc
   're-core.features.ec2))

(defn run-tests []
  (clojure.test/run-tests
   're-core.test.kvm
   're-core.test.specs
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
   're-core.features.lxc
   ;; 're-core.features.digital
   ))

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
