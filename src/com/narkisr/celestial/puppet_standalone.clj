(ns com.narkisr.celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:import com.jcraft.jsch.JSchException)
  (:use 
    clojure.core.strint
    com.narkisr.celestial.core
    clj-ssh.ssh
    [taoensso.timbre :only (debug info error warn)]
    [clojure.string :only (join)]
    [slingshot.slingshot :only  [throw+ try+]]))


(def ssh-opts {:username "root" :strict-host-key-checking :no})

(defn with-session [host f]
  (let [session (session (ssh-agent {}) host ssh-opts)] 
    (try+
      (with-connection session (f session))
      (catch #(= (.getMessage %) "Auth fail") e
        (throw+ {:type ::auth :host host} "Failed to login make sure to ssh-copy-id to the remote host"))
      )))

(defn put [host file dest]
  (with-session host
    (fn [session]
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest)
          )))))

(defn copy [server module]
  (put (:host server) (str (module :src) (module :name) ".tar.gz")  "/tmp"))

(defn execute [{:keys [host]} & batch]
  (with-session host 
    (fn [session]
      (with-connection session
        (let [{:keys [out exit] :as result} (ssh session {:in  (join "\n" batch ) })]
          (if (= exit 0)
            (debug out)
            (throw+ (assoc result :type ::exec) "Failed to execute remotly")))))))


(defn extract [server module]
  (execute server "cd /tmp" (<< "tar -xvzf ~(:name module).tar.gz"))) 

(defn cleanup [server module]
  (execute server "cd /tmp" (<< "rm -rf ~(:name module)*")))

(defn run [server module]
  (execute server (<< "cd /tmp/~(:name module)") "./run.sh"))

(deftype Standalone [server module]
  Provision
  (apply- [this]
    (use 'com.narkisr.celestial.puppet-standalone)
    (use 'com.narkisr.celestial.core)
    (copy server module)
    (extract server module)
    (run server module)
    (cleanup server module)
    ))

#_(.apply 
    (Standalone. {:host "192.168.5.203"} {:name "puppet-base-env" :src "/home/ronen/code/"}))

