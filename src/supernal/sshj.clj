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
  (let [ssh (SSHClient.)]
    (.addHostKeyVerifier ssh (PromiscuousVerifier.))
    (.loadKnownHosts ssh)
    (.connect ssh host)
    (.authPublickey ssh user #^"[Ljava.lang.String;" (into-array [(@config :key)]) )
    ssh))

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
    (let [session (.startSession ssh) res (.exec session cmd) ]
      (debug (<< "[~(remote :host)]:") cmd) 
      (log-output (.getInputStream res) (remote :host)))))

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
