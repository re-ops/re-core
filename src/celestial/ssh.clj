(ns celestial.ssh
  (:use 
    [celestial.common :only (config)]
    [taoensso.timbre :only (debug info error warn)]
    [clojure.string :only (join)]
    [slingshot.slingshot :only  [throw+ try+]]
    clj-ssh.ssh))

(def #^ { :doc "SSH session options"}
  ssh-opts {:username "root" :strict-host-key-checking :no :port (config :ssh-port)})

(defn with-session [host f]
  "Executes f on host with an ssh session"
  (let [session (session (ssh-agent {}) host ssh-opts)] 
    (try+
      (with-connection session (f session))
      (catch #(= (:message %) "Auth fail") e
        (throw+ {:type ::auth :host host} 
                "Failed to login make sure to ssh-copy-id to the remote host")))))

(defn put [host file dest]
  "Copies a file into host under dest"
  (with-session host
    (fn [session]
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest))))))

(defn copy [server module]
  "Copy puppet module into server"
  (put (:host server) (str (module :src) (module :name) ".tar.gz")  "/tmp"))

(defn log-output [out]
  "Output log stream"
  (doseq [line (line-seq (clojure.java.io/reader out))] (debug line) )) 

(defn execute [{:keys [host]} & batches]
  {:pre [(every? sequential? batches)]}
  "Executes remotly using ssh for example: (execute {:host \"192.168.20.171\"} [\"ls\"])"
  (with-session host
    (fn [session]
      (doseq [b batches]
        (let [{:keys [channel out-stream] :as res} (ssh session {:in  (join "\n" b)  :out :stream})]
          (println (bean channel))
          (log-output out-stream)
          (let [exit (.getExitStatus channel)]
            (when-not (= exit 0) 
              (throw+ (merge res {:type ::execute-failed :exit exit} (meta b))))))))))

