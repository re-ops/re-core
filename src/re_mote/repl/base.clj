(ns re-mote.repl.base
  (:require
   [re-share.core :refer (gen-uuid)]
   [clojure.java.shell :refer [sh]]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.core.incubator :refer (dissoc-in)]
   [re-mote.ssh.pipeline :refer (run-hosts upload-hosts download-hosts)]
   [re-mote.spec :refer (pipeline!)]
   [re-cog.scripts.common :refer (bind-bash)]
   [re-cog.scripts.file :as fs]
   [pallet.stevedore :refer (script)]))

(require '[re-cog.scripts.common :refer (shell-args)])

(refer-timbre)

(defn sh-hosts
  "Run a local commands against hosts"
  [{:keys [auth hosts]} sh-fn]
  (let [results (map (fn [host] (assoc (sh-fn host) :host host)) hosts)
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(bind-bash)

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
    `(let [[this# res#] (re-mote.spec/pipeline! ~source)]
       (re-mote.spec/pipeline! (~f this# res# ~@args)))))

(defmacro run>
  "Run and return last function pipe output"
  [f p s & fns]
  (if-not (empty? fns)
    `(run> (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defmacro run
  "Run and nullify output"
  [f p s & fns]
  `(let [[this# m#] (run> ~f ~p ~s ~@fns)]
     [this# {}]))

(defn safe-output [{:keys [out err exit]}]
  (when (seq out)
    (debug out))
  (when-not (zero? exit)
    (error err exit))
  {:code exit :error {:out out} :uuid (gen-uuid)})

(def safe (comp safe-output sh))

(defprotocol Shell
  (exec [this script])
  (nohup [this script])
  (null [this m])
  (rm
    [this target flags]
    [this m target flags])
  (ls [this target flags])
  (grep [this expr flags])
  (mkdir [this folder flags])
  (cp [this src dest flags])
  (purge [this n dest]))

(defprotocol Tracing
  (ping [this target]))

(defprotocol Copy
  (scp-into
    [this src dest]
    [this m src dest])
  (scp-from
    [this src dest]
    [this m src dest])
  (sync-2
    [this src dest]
    [this m src dest])
  (sync-
    [this src dest]
    [this m src dest]))

(defprotocol Tar
  (extract [this m archive target]))

(defprotocol Performance
  (measure [this m]))

(defprotocol Select
  (initialize [this])
  (downgrade
    [this f]
    [this m f]
    [this m f args])
  (pick
    [this f]
    [this m f]))

(defprotocol Transform
  (convert [this m f]))

(defn rsync [src target host {:keys [user ssh-key]}]
  (let [opts (if ssh-key (<< "-ae 'ssh -i ~{ssh-key}'") "-a")
        dest (<< "~{user}@~{host}:~{target}")]
    (script ("rsync" "--delete" ~opts  ~src  ~dest))))

(defn merge-results [[_ {:keys [success failure] :as res}] m]
  (-> m
      (dissoc-in [:failure -1])
      (clojure.core/update :success (partial into success))
      (clojure.core/update :failure (partial merge-with conj failure))))

(defrecord Hosts [auth hosts]
  Select
  (initialize [this]
    [this hosts])

  (pick [this f]
    (Hosts. auth (filter f hosts)))

  (pick [this {:keys [failure success] :as m} f]
    (let [hs (f success failure hosts)]
      (if (empty? hs)
        (throw (ex-info "no succesful hosts found" m))
        [(Hosts. auth hs) m])))

  (downgrade [this f]
    [this {}])

  (downgrade [this {:keys [failure] :as m} f]
    (downgrade this m f nil))

  (downgrade [this {:keys [failure] :as m} f args]
    (let [failed (map :host (get failure -1))]
      (if-not (empty? failed)
        [this (merge-results (apply f (Hosts. auth failed) args) m)]
        [this m])))

  Transform
  (convert [{:keys [auth hosts]} m f]
    [(Hosts. auth (mapv f hosts)) m])

  Shell
  (ls [this target flags]
    [this (run-hosts this (script ("ls" ~flags ~target)))])

  (mkdir [this folder flags]
    [this (run-hosts this (script ("mkdir" ~flags ~folder)))])

  (rm [this target flags]
    [this (run-hosts this (script ("rm" ~flags ~target)))])

  (rm [this _ target flags]
    (rm this target flags))

  (exec [this script]
    [this (run-hosts this script)])

  (nohup [this cmd]
    (exec this (<< "nohup sh -c '~{cmd} &' &>/dev/null")))

  (purge [this n dest]
    [this (run-hosts this (fs/purge n dest))])

  (null [this m]
    [this {}])

  Tar
  (extract [this _ archive target]
    [this (run-hosts this (script ("tar" "-xzf" ~archive "-C" ~target)))])

  Copy
  (scp-into [this _ src target]
    (scp-into this src target))

  (scp-into [this src target]
    [this (upload-hosts this src target)])

  (scp-from [this _ src target]
    (scp-from this src target))

  (scp-from [this src target]
    [this (download-hosts this src target)])

  (sync- [this _ src target]
    (sync- this src target))

  (sync- [{:keys [auth hosts] :as this} src target]
    [this (sh-hosts this (fn [host] (safe "sh" "-c" (rsync src target host auth))))])

  (sync-2 [this _ src target]
    (sync-2 this src target))

  (sync-2 [{:keys [auth hosts] :as this} src target]
    [this (run-hosts this (script ("rsync" "--delete" "-a" ~src ~target)))])

  Tracing
  (ping [this target]
    [this (run-hosts this (script ("ping" "-c" 1 ~target)))]))

(defn successful
  "Used for picking successful"
  [success _ hs]
  (filter (set (map :host success)) hs))

(defn refer-base []
  (require '[re-mote.repl.base :as base :refer
             (run> run | initialize pick successful ping ls convert exec scp-into
                   scp-from extract rm nohup mkdir sync- sync-2 downgrade null)]))
