(ns com.narkisr.celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:use 
    clojure.core.strint
    com.narkisr.celestial.core
    clj-ssh.ssh
    [slingshot.slingshot :only  [throw+ try+]]
    [taoensso.timbre :only (debug info error warn)]
    [clojure.string :only (join)]
    ))


(defn put [host file dest]
  (let [agent (ssh-agent {}) session (session agent host {:username "root"}) ]
    (with-connection session
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest)
          )))))

(defn copy [server module]
  (put (:host server) (str (module :src) (module :name) ".tar.gz")  "/tmp"))

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
       (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*")))))

(.apply-
    (Standalone. {:host "192.168.5.203"} {:name "puppet-base-env" :src "/home/ronen/code/"}))

