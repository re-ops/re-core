(ns supernal.baseline
  "A group of base recipes for deployment etc.."
  (:import java.util.Date)
  (:use 
    [clojure.string :only (split)]
    [clojure.core.strint :only (<<)]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- lifecycle copy run)]))

(def run-ids (atom {}))

(defn date-fmt []
  (.format (java.text.SimpleDateFormat. "HH-mm-ss_yyyy-MM-dd") (Date.)))

(defn releases [name* id] (<< "/u/apps/~{name*}/releases/~(@run-ids id)/"))

(defn current [name*] (<< "/u/apps/~{name*}/current"))

(defn archive-types [src dst] 
  {"zip" (<< "unzip ~{src} -d ~{dst}") "gz" (<< "tar -xzf ~{src} -C ~{dst}")})

(defn archive? [file]
   ((into #{} (keys (archive-types nil nil))) (last (split file #"\."))))

(ns- deploy 
  (task update-code
    (let [{:keys [src app-name run-id]} args]
      (debug "updating code on" remote) 
      (copy src (releases app-name run-id)))) 
 
  (task post-update
    (let [{:keys [src app-name run-id]} args file (last (split src #"/")) 
          basepath (releases app-name run-id)]
      (when-let [ext (archive? file)]
        (debug ext)
        (run ((archive-types (<< "~{basepath}~{file}") basepath) ext)))))

  (task start 
    (debug "starting service on" remote)) 
 
  (task symlink
    (let [{:keys [app-name run-id]} args]
      (run (<< "rm -f ~(current app-name)"))
      (run (<< "ln -s ~(releases app-name run-id) ~(current app-name)"))))

  (task stop
    (debug "stopping service on" remote))
     
  (task pre-update
    (let [{:keys [app-name run-id]} args release-id (date-fmt)]
      (swap! run-ids assoc run-id release-id)
      (run (<< "mkdir ~(releases app-name run-id) -p"))
      (run (<< "chown ~(remote :user) ~(releases app-name run-id)"))))) 


(lifecycle basic-deploy
  {deploy/update-code #{deploy/post-update deploy/symlink}
   deploy/stop #{deploy/update-code}
   deploy/pre-update #{deploy/update-code}
   deploy/symlink #{deploy/start} 
   deploy/post-update #{deploy/start}
   deploy/start #{}})

(lifecycle basic-rollback {})
