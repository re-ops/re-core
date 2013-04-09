(ns capistrano.remoter
  (:use 
    [clojure.java.shell :only (with-sh-dir)]
    [supernal.sshj :only (copy sh-)]
    [clojure.java.io :only (file)]
    [celestial.common :only (import-logging)]
    [clojure.core.strint :only (<<)]
    [clojure.java.shell :only [sh]]
    [trammel.core :only  (defconstrainedrecord)]
    [celestial.core :only (Remoter)]
    [celestial.model :only (rconstruct)]))

(import-logging)

(defconstrainedrecord Capistrano [src args name]
  "A capistrano remote"
  []
  Remoter
  (run [this context]
       (copy src "/tmp")
       (with-sh-dir (<< "/tmp/~{name}")
         (apply sh- (into ["cap"] args)))))

(defmethod rconstruct :capistrano [spec]
  (let [{:keys [src args name] :as spec} (spec :capistrano)]
    (->Capistrano src args name)))

