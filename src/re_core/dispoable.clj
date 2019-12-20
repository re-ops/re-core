(ns re-core.dispoable
  "Dispoable VMs functions"
  (:require
   [clojure.java.io :refer (file)]
   [re-core.repl :refer (spice-into matching systems hosts destroy typed)]
   [re-core.repl.systems :as sys :refer (refer-systems)]
   [re-core.repl.base :refer (refer-base)]
   [re-core.presets.systems :as sp]
   [re-mote.repl :refer (open-file)])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)

(def pdfs
  (.getPathMatcher (java.nio.file.FileSystems/getDefault) "glob:*.{pdf}"))

(defn files [root matcher]
  (->> (file root)
       file-seq
       (filter #(.isFile %))
       (filter #(.matches matcher (.getFileName (.toPath %))))
       (mapv #(.getAbsolutePath %))))

(defn dispoable
  "Open a file/url in a dispoable VM"
  [root & opts]
  (let [ms (sp/dispoable-instance)
        [_ m] (run (add- systems (ms true)) | (sys/create) | (block-wait))
        {:keys [system-id]} (-> m :results :success first :args first)]
    (spice-into (matching system-id))
    (open-file (hosts (matching system-id) :ip) root)))

(defn dispose
  "Clear all dispoable instances"
  []
  (destroy (typed :disposable) {:force true}))
