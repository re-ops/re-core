(ns re-core.repl.systems
  "Repl systems access"
  (:require
   kvm.provider
   [clojure.string :refer (lower-case split)]
   [clojure.core.strint :refer  (<<)]
   [re-core.model :refer (vconstruct sconstruct)]
   [re-share.config :refer  (get!)]
   [clansi.core :refer  (style)]
   [re-core.queue :as q]
   [re-core.common :refer (gen-uuid)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.set :refer (difference)]
   [es.systems :as s]
   [es.jobs :as es]
   [re-mote.repl.base :as base]
   [re-mote.repl :refer (host-hardware-info host-os-info)]
   [re-core.repl.types :refer (provision-type)]
   [com.rpl.specter :refer [transform ALL multi-path select MAP-VALS nthpath]]
   [re-core.repl.base :refer [Repl Report select-keys*]])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Systems]))

(refer-timbre)

(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (start [this items])
  (create [this items])
  (provision [this items])
  (destroy [this items])
  (reload [this items])
  (status [this jobs])
  (block-wait [this jobs])
  (async-wait [this jobs f args])
  (pretty-print [this m message])
  (spice [this items]))

(defprotocol Host
  "Hosts"
  (into-hosts
    [this items]
    [this items k]))

(defprotocol Synching
  "Hosts"
  (synch
    [this hyp opts])
  (add-hosts-info
    [this items opts]))

(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))))

(defn run-ack [this {:keys [systems] :as m}]
  (println "The following systems will be effected Y/n (n*):")
  (doseq [[id s] systems]
    (println "      " id (get-in s [:machine :hostname])))
  (if-not (= (read-line) "Y")
    [this {}]
    [this m]))

(extend-type Systems
  Repl
  (ls [this]
    [this {:systems (s/all)}])

  (filter-by [this {:keys [systems] :as m} f]
    [this {:systems (filter f systems)}])

  (ack [this {:keys [systems] :as m} opts]
    (if-not (contains? opts :force)
      (run-ack this m)
      [this m]))

  (rm [this {:keys [systems] :as m}]
    (doseq [id (map first systems)]
      (s/delete id))
    [this {:systems []}])

  (grep [this {:keys [systems] :as m} k v]
    [this {:systems (filter (partial grep-system k v) (systems :systems))}])

  (add- [this specs]
    (let [f (fn [s] (let [id (s/create s)] [id (assoc (s/get id) :system-id id)]))]
      [this {:systems (map f specs)}])))

(defn filter-done [sts]
  (into #{} (filter (fn [{:keys [status]}] (not (nil? status))) sts)))

(defn with-id
  [[id system]]
  [(assoc system :system-id id)])

(defn- schedule-job [topic args]
  (let [tid (gen-uuid) job {:tid tid :args args}]
    (q/enqueue topic job)
    {:job job}))

(defn with-jobs [m topic js]
  (merge m {:jobs js :topic topic}))

(defn- schedule
  ([m topic systems]
   (schedule m topic systems with-id))
  ([m topic args f]
   (with-jobs m topic
     (map (fn [a] (schedule-job topic (f a))) args))))

(defn result [{:keys [job]}]
  (merge (es/get (job :tid)) job))

(defn add-results [this jobs]
  (let [{:keys [success] :as results} (group-by (comp keyword :status) (map result jobs))]
    {:systems (map :systems success) :results results}))

(extend-type Systems
  Jobs
  (stop [this {:keys [systems] :as m}]
    [this (schedule m "stop" systems)])

  (start [this {:keys [systems] :as m}]
    [this (schedule m "start" systems)])

  (create [this {:keys [systems] :as m}]
    [this (schedule m "create" systems)])

  (provision [this {:keys [systems] :as m}]
    (let [by-type (group-by (comp :type second) systems)
          into-args (fn [[t ms]] [(flatten (mapv with-id ms)) (provision-type t)])]
      [this (schedule m "provision" by-type into-args)]))

  (reload [this {:keys [systems] :as m}]
    [this (schedule m "reload" systems)])

  (destroy [this {:keys [systems] :as m}]
    [this (schedule m "destroy" systems)])

  (status [this {:keys [jobs]}]
    (map (fn [{:keys [job]}] (assoc job :status (q/status job))) jobs))

  (block-wait [this {:keys [jobs systems] :as js}]
    (loop [done (filter-done (status this js))]
      (when (< (count done) (count jobs))
        (Thread/sleep 100)
        (recur (filter-done (status this js)))))
    [this (add-results this jobs)])

  (async-wait [this {:keys [jobs systems] :as js} f & args]
    (let [out *out*]
      (future
        (binding [*out* out]
          (loop [done (filter-done (status this js))]
            (when (< (count done) (count jobs))
              (Thread/sleep 100)
              (recur (filter-done (status this js)))))
          (apply f (into [this (add-results this jobs)] args))))))

  (pretty-print [this {:keys [results chain] :as m} message]
    (let [{:keys [success failure]} results]
      (println "\n")
      (println (style (<< "Running ~{message} summary:") :blue) "\n")
      (doseq [{:keys [message args]} success :let [hostname (get-in args [0 :machine :hostname])]]
        (println " " (style "✔" :green) hostname))
      (doseq [{:keys [message args]} failure :let [hostname (get-in args [0 :machine :hostname])]]
        (println " " (style "x" :red) hostname "-" message))
      (println "")
      [this m]))

  (spice [this {:keys [systems] :as m}]
    (doseq [[_ s] systems :let [k (vconstruct s)]]
      (kvm.provider/open-spice k))
    [this m]))

(extend-type Systems
  Host
  (into-hosts [this m]
    (into-hosts this m :ip))
  (into-hosts [this {:keys [systems]} k]
    (let [{:keys [user]} (:machine (second (first systems)))
          remote {:user user :ssh-key (get! :shared :ssh :private-key-path)}]
      (Hosts. remote (mapv (fn [[_ system]] (get-in system [:machine k])) systems)))))

(extend-type Systems
  Report
  (summary [this {:keys [success failure] :as m}]
    (println "")
    (println (style "Run summary:" :blue) "\n")
    (doseq [{:keys [identity topic]} success]
      (println " " (style "✔" :green) topic identity))
    (doseq [{:keys [identity topic message]} failure]
      (println " " (style "x" :red) topic identity "-" message))
    (println "")
    [this m]))

(defn persist-synched [systems]
  (map
   (fn [system]
     (let [id (s/create system)] [id system])) (s/missing-systems systems)))

(defn os-versions [[_ {:keys [success]}]]
  (apply merge
         (transform [ALL]
                    (fn [{:keys [host result]}]
                      (let [{:keys [version family]} result
                            release (first (split (version :version) #"\s"))]
                        {host (keyword (<< "~(lower-case family)-~{release}"))})) success)))

(defn match-ip [subnet {:keys [ipv4]}]
  (when-not (empty? ipv4)
    (.startsWith (first ipv4) subnet)))

(defn hosts-ip
  "Find host public ip by subnet"
  [subnet [_ {:keys [success]}]]
  (let [interfaces (apply hash-map (select [ALL (multi-path [:host] [:result :networks])] success))]
    (transform [MAP-VALS]
               (fn [nics]
                 (-> (first (filter (partial match-ip subnet) nics)) :ipv4 first)) interfaces)))

(defn add-os [systems versions]
  (map
   (fn [[id {:keys [machine] :as m}]]
     (let [{:keys [hostname os]} machine]
       [id (assoc-in m [:machine :os] (or (versions hostname) os))])) systems))

(extend-type Systems
  Synching
  (synch
    [this hyp opts]
    (let [syncher (sconstruct hyp opts)
          systems (persist-synched (.sync syncher))]
      [this {:systems systems}]))

  (add-hosts-info
    [this {:keys [systems] :as m} {:keys [subnet]}]
    (let [hs (into-hosts this m :hostname)
          versions (os-versions (host-os-info hs))
          ips (hosts-ip subnet (host-hardware-info hs))
          updated (add-os systems versions)]
      (doseq [[id system] updated]
        (s/put id system))
      [this {:systems updated}])))

(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer [status into-hosts block-wait async-wait pretty-print spice synch add-hosts-info]]))
