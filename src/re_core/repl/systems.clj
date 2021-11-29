(ns re-core.repl.systems
  "Repl systems access"
  (:require
   kvm.provider
   [re-core.presets.systems :as sp]
   [clojure.string :refer (lower-case split)]
   [clojure.core.strint :refer (<<)]
   [re-core.model :refer (vconstruct sconstruct)]
   [re-share.config.core :refer  (get!)]
   [clansi.core :refer  (style)]
   [re-core.queue :as q]
   [re-share.core :refer (gen-uuid)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-core.persistency.systems :as s]
   [es.jobs :as es]
   [com.rpl.specter :refer [transform ALL multi-path select MAP-VALS nthpath]]
   [re-core.repl.base :as base :refer [Repl Report select-keys* pretty]]
   [re-core.repl.results :as r])
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
  "Synching existing systems into re-core"
  (synch
    [this hyp opts])
  (update-systems
    [this systems ks v]))

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

  (valid? [this base args]
    (let [validated (sp/validate (sp/materialize-preset base args))]
      [this {:systems (validated true) :results {:failure (validated false)}}]))

  (add- [this {:keys [systems] :as m}]
    (let [f (fn [s] (let [id (s/create s)] [id (assoc (s/get id) :system-id id)]))]
      [this (assoc m :systems (map f systems))])))

(defn filter-done [sts]
  (into #{} (doall (filter (fn [{:keys [status]}] (not (nil? status))) sts))))

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

(defn add-results
  "Add the job result from ES"
  [this jobs prev-results]
  (let [{:keys [success] :as results} (group-by (comp keyword :status) (map result jobs))]
    {:systems (map :systems success) :results (merge-with merge results prev-results)}))

(extend-type Systems
  Jobs
  (stop [this {:keys [systems] :as m}]
    [this (schedule m "stop" systems)])

  (start [this {:keys [systems] :as m}]
    [this (schedule m "start" systems)])

  (create [this {:keys [systems] :as m}]
    [this (schedule m "create" systems)])

  (provision [this {:keys [systems] :as m}]
    [this (schedule m "provision" systems)])

  (reload [this {:keys [systems] :as m}]
    [this (schedule m "reload" systems)])

  (destroy [this {:keys [systems] :as m}]
    [this (schedule m "destroy" systems)])

  (status [this {:keys [jobs]}]
    (map (fn [{:keys [job]}] (assoc job :status (q/status job))) jobs))

  (block-wait [this {:keys [jobs systems results] :as js}]
    (loop [done (filter-done (status this js))]
      (when (< (count done) (count jobs))
        (Thread/sleep 100)
        (recur (filter-done (status this js)))))
    [this (add-results this jobs results)])

  (async-wait [this {:keys [jobs systems results] :as js} f & args]
    (let [out *out*]
      (future
        (binding [*out* out]
          (loop [done (filter-done (status this js))]
            (when (< (count done) (count jobs))
              (Thread/sleep 100)
              (recur (filter-done (status this js)))))
          (let [result (add-results this jobs results)]
            (r/append result)
            (apply f (into [this result] args)))))))

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
          auth {:user user :ssh-key (get! :shared :ssh :private-key-path)}]
      (Hosts. auth (mapv (fn [[_ system]] (get-in system [:machine k])) systems)))))

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

  (update-systems [this {:keys [systems] :as m} ks v]
    (let [updated (map (fn [[id system]] [id (assoc-in system ks v)]) systems)]
      (doseq [[id system] updated]
        (s/put id system))
      [this (assoc m :systems updated)])))

(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer
             [status into-hosts block-wait async-wait pretty-print spice synch update-systems]]))
