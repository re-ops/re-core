(ns re-core.dispoable
  "Dispoable VMs functions"
  (:require
   [clojure.core.strint :refer (<<)]
   [clojure.java.io :refer (file)]
   [re-core.repl :refer (spice-into matching systems hosts destroy typed)]
   [re-core.repl.systems :as sys :refer (refer-systems)]
   [re-core.repl.base :refer (refer-base)]
   [es.types :as t]
   [re-core.presets.systems :as sp]
   [re-mote.repl :refer (open-file)])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)

(def home (System/getProperty "user.home"))

(def downloads (<< "~{home}/Downloads"))

(def pdfs
  (.getPathMatcher (java.nio.file.FileSystems/getDefault) "glob:*.{pdf}"))

(defn files [root matcher]
  (->> (file root)
       file-seq
       (filter #(.isFile %))
       (filter #(.matches matcher (.getFileName (.toPath %))))
       (mapv #(.getAbsolutePath %))))

(defn pick-files
  "Selecting files by entering comma separated numbers (ie 1,2):
    (pick-files downloads pdfs)
  "
  [root type]
  (println (<< "Please select which files to view under ~{root}:"))
  (let [fs (files root type)
        indexed (map (fn [f i] [i (.replace f (<< "~{root}/") "")]) fs (range (count fs)))]
    (doseq [[i f] indexed]
      (println (<< " ~{i}. ~{f}")))
    (let [input (read-line)
          idx (map (fn [i] (Integer. i)) (clojure.string/split input #"\,"))]
      (map fs idx))))

(defn dispoable
  "Open a file/url in a dispoable VM, make sure to have a disposable type before using this function"
  [root & {:keys [type] :or {type pdfs}}]
  {:pre [(t/exists? "disposable")]}
  (let [fs (pick-files root type)
        ms (sp/dispoable-instance)
        [_ m] (run (add- systems (ms true)) | (sys/create) | (block-wait))
        {:keys [system-id]} (-> m :results :success first :args first)]
    (spice-into (matching system-id))
    (doseq [f fs]
      (open-file (hosts (matching system-id) :ip) f))))

(defn dispose
  "Clear all dispoable instances"
  []
  (destroy (typed :disposable) {:force true}))
