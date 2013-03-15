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


(defn copy-module [{:keys [host]} {:keys [src name]}]
  {:pre [host src name]}
  "Copy a opsk module into server"
  (copy host src "/tmp"))

(defn copy-yml-type [{:keys [type hostname puppet-std] :as _type} host]
  (let [path (<< "/tmp/~{hostname}.yml") f (file path)
        name (get-in puppet-std [:module :name])]
    (spit f (yaml/generate-string (select-keys _type [:classes])))
    (copy host path (<< "/tmp/~{name}/"))
    (.delete f)))

(defrecord Standalone [ip type]
  Provision
  (apply- [this]
    (use 'celestial.puppet-standalone)
    (let [puppet-std (type :puppet-std) module (puppet-std :module)]
     (try 
      (copy-module {:host ip} module) 
      (execute {:host ip}
        (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz"))) 
      (copy-yml-type type ip)
      (execute {:host ip}
          (step :run (<< "cd /tmp/~(:name module)") "./scripts/run.sh "))
      (finally 
        (execute {:host ip} (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*")))))))) 


(defmethod pconstruct :puppet-std [type {:keys [machine] :as spec}]
   (Standalone. (machine :ip) (assoc type :hostname (machine :hostname))))
