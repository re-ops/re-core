(ns capistrano.remoter
  (:use 
    [clojure.java.shell :only (with-sh-dir)]
    [supernal.sshj :only (copy sh- dest-path)]
    [clojure.java.io :only (file delete-file)]
    [celestial.common :only (import-logging)]
    [clojure.core.strint :only (<<)]
    [clojure.java.shell :only [sh]]
    [trammel.core :only  (defconstrainedrecord)]
    [celestial.core :only (Remoter)]
    [celestial.model :only (rconstruct)]))

(import-logging)

(defconstrainedrecord Capistrano [src args name]
  "A capistrano remote agent"
  []
  Remoter
  (setup [this] 
    (when-not (.exists (file (dest-path "/tmp" src)))
      (copy src "/tmp")))
  (run [this context]
       (with-sh-dir (<< "/tmp/~{name}")
         (apply sh- (into ["cap"] args))))
  (cleanup [this]
       (delete-file (file (dest-path "/tmp" src)))))

(defmethod rconstruct :capistrano [spec]
  (let [{:keys [src args name] :as spec} (spec :capistrano)]
    (->Capistrano src args name)))

