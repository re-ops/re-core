(ns capistrano.remoter
  (:use 
    [clojure.core.strint :only  (<<)]
    [slingshot.slingshot :only [throw+]]
    [clojure.java.shell :only (with-sh-dir)]
    [supernal.sshj :only (copy sh- dest-path)]
    [celestial.common :only (import-logging gen-uuid interpulate)]
    [clojure.java.shell :only [sh]]
    [me.raynes.fs :only (delete-dir exists? mkdirs tmpdir)]
    [trammel.core :only  (defconstrainedrecord)]
    [celestial.core :only (Remoter)]
    [celestial.model :only (rconstruct)]))

(import-logging)

(defconstrainedrecord Capistrano [src args dst]
  "A capistrano remote agent"
  []
  Remoter
  (setup [this] 
         (when (exists? (dest-path src dst)) 
           (throw+ {:type ::old-code :message "Old code found in place, cleanup first"})) 
         (mkdirs dst) 
         (try (with-sh-dir dst (sh- "cap" "-T"))
           (catch Throwable e
             (throw+ {:type ::cap-not-found :message "Capistrano binary not found in path"})))
         (copy src dst))
  (run [this]
       (info (dest-path src dst))
       (with-sh-dir (dest-path src dst)
         (apply sh- (into ["cap"] args))))
  (cleanup [this]
           (delete-dir dst)))

(defmethod rconstruct :capistrano [{:keys [actions src] :as spec} 
                                   {:keys [action] :as run-info}]
  (let [task (get-in actions [action :capistrano])]
    (->Capistrano src (mapv #(interpulate % run-info) (task :args)) (<< "~(tmpdir)/~(gen-uuid)/~(name action)"))))


