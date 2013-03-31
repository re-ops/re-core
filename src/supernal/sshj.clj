(ns supernal.sshj
  (:use 
    [celestial.topsort :only (kahn-sort)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (import-logging)]
    [plumbing.core :only (defnk)] 
    ) 
  (:import 
    (net.schmizz.sshj.common StreamCopier$Listener)
    (net.schmizz.sshj.xfer FileSystemFile TransferListener)
    (net.schmizz.sshj SSHClient)
    (net.schmizz.sshj.userauth.keyprovider FileKeyProvider)
    (net.schmizz.sshj.transport.verification PromiscuousVerifier)
    ))

(import-logging)

(defn default-config []
  {:key (<< "~(System/getProperty \"user.home\")/.ssh/id_rsa" ) :user "root" })

(def config (atom (default-config)))

(defn log-output 
  "Output log stream" 
  [out host]
  (doseq [line (line-seq (clojure.java.io/reader out))] (debug  (<< "[~{host}]:") line)))

(defnk ssh-strap [host {user (@config :user)}]
  (doto (SSHClient.)
    (.addHostKeyVerifier (PromiscuousVerifier.))
    (.loadKnownHosts )
    (.connect host)
    (.authPublickey user #^"[Ljava.lang.String;" (into-array [(@config :key)]))))

(defmacro with-ssh [remote & body]
  `(let [~'ssh (ssh-strap ~remote)]
     (try 
       ~@body
       (catch Throwable e#
         (error e#)
         (.disconnect ~'ssh)))))

(defn execute 
  "Executes a cmd on a remote host"
  [cmd remote]
  (with-ssh remote 
    (let [session (doto (.startSession ssh) (.allocateDefaultPTY)) command (.exec session cmd) ]
      (debug (<< "[~(remote :host)]:") cmd) 
      (log-output (.getInputStream command) (remote :host))
      (log-output (.getErrorStream command) (remote :host))
      (.join command 60 TimeUnit/SECONDS) 
      (when-not (= 0 (.getExitStatus command))
        (throw (Exception. (<< "Failed to execute ~{cmd} on ~{remote}"))))
      )))

(def listener 
  (proxy [TransferListener] []
    (directory [name*] (debug "starting to transfer" name*)) 
    (file [name* size]
      (proxy [StreamCopier$Listener ] []
        (reportProgress [transferred]
          (debug (<< "transferred ~(float (/ (* transferred 100) size))% of ~{name*}")))))))

(defn upload [src dst remote]
  (with-ssh remote
    (let [scp (.newSCPFileTransfer ssh)]
      (.setTransferListener scp listener)
      (.upload scp (FileSystemFile. src) dst) 
      )))

; (execute "ping -c 1 google.com" {:host "localhost" :user "ronen"}) 
; (upload "/home/ronen/Downloads/PCBSD9.1-x64-DVD.iso" "/tmp" {:host "localhost" :user "ronen"})
