(ns celestial.ssh
  (:use 
    [clojure.core.strint :only (<<)]
    [clojure.string :only (split)]
    [celestial.common :only (config)]
    [taoensso.timbre :only (debug info error warn)]
    [clojure.string :only (join)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clj-ssh.ssh :only 
      (session ssh-agent with-connection sftp ssh-sftp with-channel-connection ssh)]))

(def #^ { :doc "SSH session options"}
  ssh-opts {:username "root" :strict-host-key-checking :no :port (config :ssh-port)})

(defn with-session 
  "Executes f on host with an ssh session"
  [host f]
  (let [session (session (ssh-agent {}) host ssh-opts)] 
    (try+
      (with-connection session (f session))
      (catch #(= (:message %) "Auth fail") e
        (throw+ {:type ::auth :host host} 
                "Failed to login make sure to ssh-copy-id to the remote host")))))

(defn put 
  "Copies a file into host under dest"
  [host file dest]
  (with-session host
    (fn [session]
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest))))))

(defn log-output 
  "Output log stream" 
  [out]
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

(defn fname [uri] (-> uri (split '#"/") last))

(defmulti copy 
  "A general remote copy" 
  (fn [_ uri _] 
    (keyword (first (split uri '#":")))))

(defmethod copy :git  [host uri dest] (debug "do remote clone"))
(defmethod copy :http [host uri dest] (execute {:host host} [(<< "wget -O ~{dest}/~(fname uri) ~{uri}")]))
(defmethod copy :file [host uri dest] (put host (fname uri)  dest))
(defmethod copy :default [host uri dest] (copy host (<< "file:/~{uri}") dest))

