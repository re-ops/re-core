(ns celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:import com.jcraft.jsch.JSchException)
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Provision)]
    [celestial.ssh :only (copy execute)]
    [taoensso.timbre :only (debug info error warn)]
    [slingshot.slingshot :only  [throw+ try+]]))

(defn step [n & steps] (with-meta steps {:step n}))

(defn copy-module [{:keys [host]} {:keys [src name]}]
  "Copy a puppet module into server"
  (copy host src  "/tmp"))

(defrecord Standalone [server module]
  Provision
  (apply- [this]
    (copy-module server module) 
    (execute server 
      (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz")) 
      (step :run (<< "cd /tmp/~(:name module)") "./run.sh")
      (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*"))) ))


#_(.apply-
    (Standalone. {:host "192.168.5.203"} {:name "puppet-base-env" :src "/home/ronen/code/"}))

