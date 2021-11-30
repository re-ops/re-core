(ns re-core.dispoable
  "Dispoable VMs functions"
  (:require
   [clojure.core.strint :refer (<<)]
   [me.raynes.fs :as fs]
   [clojure.java.io :refer (file)]
   [re-core.repl :refer (spice-into matching systems hosts destroy typed)]
   [re-core.repl.systems :as sys :refer (refer-systems)]
   [re-core.repl.base :refer (refer-base)]
   [re-core.persistency.types :as t]
   [re-flow.core :refer (trigger)])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Types Systems]))

(refer-base)
(refer-systems)

(def home (System/getProperty "user.home"))

(def downloads (<< "~{home}/Downloads"))

(defn glob [g]
  (.getPathMatcher (java.nio.file.FileSystems/getDefault) (str "glob:*.{" g "}")))

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

(defn url? [s]
  (try
    (.toURI (java.net.URL. s))
    (catch Exception e
      false)))

(defn disposable
  "Open a file/url in a dispoable VM, make sure to have a disposable type before using this function"
  [root & {:keys [ext] :or {ext "pdf"} :as m}]
  {:pre [(t/exists? "disposable")]}
  (cond
    (fs/directory? root) (doseq [file (pick-files root (glob ext))]
                           (trigger {:state :re-flow.disposable/start
                                     :flow :re-flow.disposable/disposable
                                     :target file}))
    (url? root) (trigger {:state :re-flow.disposable/start
                          :flow :re-flow.disposable/disposable
                          :target root})
    :else (throw
           (ex-info
            (<< "no matching disposable handler found for ~{root}") {:root root :m m}))))

(defn dispose
  "Clear all dispoable instances"
  []
  (destroy (typed :disposable) {:force true}))
