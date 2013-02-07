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

#_(defn execute [{:keys [host]} & batch]
  (with-session host 
    (fn [session]
      (with-connection session
        (let [{:keys [out exit] :as result} (ssh session {:in  (join "\n" batch ) })]
          (if (= exit 0)
            (debug out)
            (throw+ (assoc result :type ::exec) "Failed to execute remotly")))))))

(defn execute [server & batches]
  (let [agent (ssh-agent {}) session (session agent (server :host) {:username "root"}) ]
    (with-connection session
      (doseq [b batches]
        (let [{:keys [exit out] :as res} (ssh session {:in  (join "\n" b) })]
          (if (= exit 0)
            (debug out) 
            (throw+ (merge res {:type ::provision-failed} (meta b)))
            ))))))

(defn step [n & steps] ^{:step n} steps)

(deftype Standalone [server module]
  Provision
  (apply- [this]
    (use 'com.narkisr.celestial.puppet-standalone)
    (use 'com.narkisr.celestial.core)
    (copy server module) 
    (execute server 
       (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz")) 
       (step :run (<< "cd /tmp/~(:name module)") "./run.sh")
       (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*"))) ))


#_(.apply-
    (Standalone. {:host "192.168.5.203"} {:name "puppet-base-env" :src "/home/ronen/code/"}))

