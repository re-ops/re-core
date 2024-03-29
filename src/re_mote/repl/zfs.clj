(ns re-mote.repl.zfs
  "A bunch of function for ZFS automation"
  (:require
   [re-share.time :refer [local-now format-time]]
   [clojure.core.strint :refer (<<)]
   [pallet.stevedore :refer (script)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]))

(refer-base)
(refer-out)

(defn scrub [hs pool]
  (run (exec hs (<< "sudo /sbin/zpool scrub ~{pool}")) | (pretty "scrub")))

(def errors "'(DEGRADED|FAULTED|OFFLINE|UNAVAIL|REMOVED|FAIL|DESTROYED|corrupt|cannot|unrecover)'")

(defn healty [pool errors]
  (script
   (pipe ("/sbin/zpool" "status" ~pool) ("egrep" "-v" "-i" ~errors))))

(defn cap-with-range [maximum]
  (script
   (set! used @("/sbin/zpool" "list" "-H" "-o" "capacity" | "cut" "-d'%'" "-f1"))
   (if (>= @used ~maximum)
     (chain-and (println "used capacity is too high" @used "maximum allowed is" ~maximum) ("exit" 1))
     ("exit" 0))))

(defn purging
  "Purge snapshots script this requires the user to have the following zfs permissions delegated to him:
      zfs allow -u <your user> destroy,hold,mount pool/dataset
   "
  [pool dataset n]
  (let [n+ (str "+" n) from (str pool "/" dataset)]
    (script
     (pipe
      (pipe
       ("zfs" "list" "-H" "-t" "snapshot" "-o" "name" "-S" "creation" "-d1" ~from)
       ("tail" "-n" ~n+))
      ("xargs" "-r" "-n" "1" "zfs" "destroy" "-r")))))

(defn health [hs pool]
  (run> (exec hs (healty pool errors)) | (pretty "health")))

(defn capacity [hs maximum]
  (run> (exec hs (cap-with-range maximum)) | (pretty "capacity")))

(defn snapshot [hs pool dataset]
  (let [date (format-time "dd-MM-YYYY_hh:mm:ss_SS" (local-now))]
    (run> (exec hs (<< "/sbin/zfs snapshot ~{pool}/~{dataset}@~{date}")) | (pretty "snapshot"))))

(defn purge
  "clear last n snapshots of a dataset"
  [hs pool dataset n]
  (run> (exec hs (purging pool dataset n)) | (pretty "purge")))

(defn refer-zfs []
  (require '[re-mote.repl.zfs :as zfs :refer (health snapshot scrub capacity purge)]))
