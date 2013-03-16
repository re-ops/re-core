(ns celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:import com.jcraft.jsch.JSchException)
  (:use 
    [clj-yaml.core :as yaml]
    [clojure.java.io :only (file)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Provision)]
    [celestial.model :only (pconstruct)]
    [celestial.ssh :only (copy execute step)]
    [taoensso.timbre :only (debug info error warn)]
    ))


(defn copy-module [remote {:keys [src name]}]
  {:pre [(remote :host) src name]}
  "Copy a opsk module into server"
  (copy remote src "/tmp"))

(defn copy-yml-type [{:keys [type hostname puppet-std] :as _type} remote]
  (let [path (<< "/tmp/~{hostname}.yml") f (file path)
        name (get-in puppet-std [:module :name])]
    (spit f (yaml/generate-string (select-keys _type [:classes])))
    (copy remote path (<< "/tmp/~{name}/"))
    (.delete f)))

(defrecord Standalone [remote type]
  Provision
  (apply- [this]
    (let [puppet-std (type :puppet-std) module (puppet-std :module)]
     (try 
      (copy-module remote module) 
      (execute remote 
        (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz"))) 
      (copy-yml-type type remote)
      (execute remote
          (step :run (<< "cd /tmp/~(:name module)") "./scripts/run.sh "))
      (finally 
        (execute remote (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*")))))))) 


(defmethod pconstruct :puppet-std [type {:keys [machine] :as spec}]
  (let [remote {:host (or (machine :ip) (machine :ssh-host)) :user (or (machine :user) "root")}]
    (Standalone. remote (assoc type :hostname (machine :hostname)))))
