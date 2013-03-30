(ns celestial.sshj
  (:use 
    [celestial.topsort :only (kahn-sort)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (import-logging)]
    [plumbing.core :only (defnk)] 
    ) 
  (:import 
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
  [out]
  (doseq [line (line-seq (clojure.java.io/reader out))] (debug line)))

(defnk session [host {user (@config :user)}]
  (let [ssh (SSHClient.)]
     (.addHostKeyVerifier ssh (PromiscuousVerifier.))
     (.loadKnownHosts ssh)
     (.connect ssh host)
     (.authPublickey ssh user #^"[Ljava.lang.String;" (into-array [(@config :key)]) )
     (.startSession ssh)))

(defn ssh-execute [cmd remote]
   (let [session (session remote) res (.exec session cmd) ]
     (debug cmd)
     (log-output (.getInputStream res))))

; (ssh-execute "ping -c 1 google.com" {:host "localhost" :user "ronen"})

