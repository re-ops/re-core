(ns re-core.dispoable
  "Dispoable VMs functions"
  (:require
   [re-core.networking :refer (ips-available)]
   [clojure.core.strint :refer (<<)]
   [me.raynes.fs :as fs]
   [clojure.java.io :refer (file)]
   [re-core.repl :refer (spice-into matching systems hosts destroy typed)]
   [re-core.repl.systems :as sys :refer (refer-systems)]
   [re-core.repl.base :refer (refer-base)]
   [es.types :as t]
   [re-core.presets.systems :refer (dispoable-instance kvm)]
   [re-mote.repl :refer (open-file browse-to)])
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

(defn url? [s]
  (try
    (.toURI (java.net.URL. s))
    (catch Exception e
      false)))

(defn open-single
  "Open a single file in a disposable instance"
  [f]
  {:pre [(t/exists? "disposable")]}
  (let [[_ m] (run (valid? systems kvm dispoable-instance) | (add-) | (sys/create) | (block-wait))
        {:keys [system-id]} (-> m :results :success first :args first)]
    (spice-into (matching system-id))
    (open-file (hosts (matching system-id) :ip) f)))

(defn open-files
  "Pick a set file types from a root directory and open them in disposable vms"
  [root {:keys [type] :or {type pdfs}}]
  {:pre [(t/exists? "disposable")]}
  (let [fs (pick-files root type)
        args dispoable-instance
        [_ m] (run (valid? systems kvm args) | (add-) | (sys/create) | (block-wait))
        {:keys [system-id]} (-> m :results :success first :args first)]
    (when-not (ips-available [system-id])
      (throw (ex-info "failed to wait for system ip to be available" m)))
    (spice-into (matching system-id))
    (doseq [f fs]
      (open-file (hosts (matching system-id) :ip) f))))

(defn open-url
  "Open a file from a provided root directory"
  [url]
  {:pre [(t/exists? "disposable")]}
  (let [args dispoable-instance
        [_ m] (run (valid? systems kvm args) | (add-) | (sys/create) | (block-wait))
        {:keys [system-id]} (-> m :results :success first :args first)]
    (when-not (ips-available [system-id])
      (throw (ex-info "failed to wait for system ip to be available" m)))
    (spice-into (matching system-id))
    (browse-to (hosts (matching system-id) :ip) url)))

(defn disposable
  "Open a file/url in a dispoable VM, make sure to have a disposable type before using this function"
  [root & m]
  {:pre [(t/exists? "disposable")]}
  (cond
    (url? root) (open-url root)
    (fs/exists? root) (open-files root m)
    :else (throw (ex-info (<< "no matching disposable handler found for ~{root}") {:root root :m m}))))

(defn dispose
  "Clear all dispoable instances"
  []
  (destroy (typed :disposable) {:force true}))
