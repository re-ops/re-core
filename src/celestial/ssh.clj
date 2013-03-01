(ns celestial.ssh
  (:use 
    [clojure.core.strint :only (<<)]
    [clojure.string :only (split)]
    [plumbing.core :only (defnk)]
    [celestial.common :only (config import-logging)]
    [clojure.string :only (join)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clj-ssh.ssh :only 
      (session ssh-agent with-connection sftp ssh-sftp with-channel-connection ssh add-identity)]))

(import-logging)

(defn ssh-opts 
  "SSH session options" 
  ([port user]
   {:username user :strict-host-key-checking :no :port port}))

(defn with-session 
  "Executes f on host with an ssh session"
  [{:keys [host port user] :or {user "root" port 22}} f]
  (let [agent (ssh-agent  {:use-system-ssh-agent false})]
    (add-identity agent (config :ssh)) 
    (let [session (session agent host (ssh-opts port user))] 
      (try+
        (with-connection session (f session))
        (catch #(= (:message %) "Auth fail") e
          (throw+ {:type ::auth :host host} 
                  "Failed to login make sure to ssh-copy-id to the remote host"))))))

(defn put 
  "Copies a file into host under dest"
  [host file dest]
  (with-session {:host host :port 22} 
    (fn [session]
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest))))))

(defn log-output 
  "Output log stream" 
  [out]
  (doseq [line (line-seq (clojure.java.io/reader out))] (debug line))
  ) 

(defn run [session batch]
   (trace "performing" (meta batch))
   (ssh session {:in (join "\n" batch) :out :stream :agent-forwarding false}))

(defn step [n & steps] (with-meta steps {:step n}))

(defn execute [opts & batches]
  {:pre [(every? sequential? batches)]}
  "Executes remotly using ssh for example: (execute {:host \"192.168.20.171\"} [\"ls\"])"
  (doseq [b batches]
    (with-session opts
      (fn [session]
        (let [{:keys [ignore-code] :or {ignore-code false}} (meta b)
              {:keys [channel out-stream] :as res} (run session b)]
          (log-output out-stream)
          (let [exit (.getExitStatus channel)]
            (when (and (not (= exit 0)) (not ignore-code)) 
              (throw+ (merge res {:type ::execute-failed :exit exit} (meta b))))))))))

(defn fname [uri] (-> uri (split '#"/") last))

(defn ^{:test #(assert (= (no-ext "celestial.git") "celestial"))}
  no-ext 
  "file name without extension"
  [name]
  (-> name (split '#"\.") first))

(defmulti copy 
  "A general remote copy" 
  (fn [_ uri _] 
    (keyword (first (split uri '#":")))))

(defmethod copy :git  [host uri dest] 
  (execute {:host host} [(<< "git clone ~{uri} ~{dest}/~(no-ext (fname uri))")]))
(defmethod copy :http [host uri dest] 
  (execute {:host host} [(<< "wget -O ~{dest}/~(fname uri) ~{uri}")]))
(defmethod copy :file [host uri dest] (put host (subs uri 6)  dest))
(defmethod copy :default [host uri dest] (copy host (<< "file:/~{uri}") dest))

(test #'no-ext)
