(ns celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:import com.jcraft.jsch.JSchException)
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Provision)]
    [celestial.ssh :only (copy execute step)]
    [taoensso.timbre :only (debug info error warn)]
    ))


(defn copy-module [{:keys [host]} {:keys [src name]}]
  {:pre [host src name]}
  "Copy a opsk module into server"
  (copy host src "/tmp"))

(defrecord Standalone [machine module]
  Provision
  (apply- [this]
    (use 'celestial.puppet-standalone)
    (try (copy-module machine module) 
      (execute machine 
        (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz")) 
        (step :run (<< "cd /tmp/~(:name module)") "./scripts/run.sh "))
      (finally 
        (execute machine (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*"))) 
        ) 
      ))) 


