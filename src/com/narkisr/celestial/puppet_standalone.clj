(ns com.narkisr.celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:use 
    clojure.core.strint
    com.narkisr.celestial.core
    clj-ssh.ssh
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

(defn execute [server & batch]
  (let [agent (ssh-agent {}) session (session agent (server :host) {:username "root"}) ]
    (with-connection session
      (let [result (ssh session {:in  (join "\n" batch ) })]
        (println result)))))


(defn extract [server module]
  (execute server "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz"))) 

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

