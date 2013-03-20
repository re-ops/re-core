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
   (let [session (session remote) cmd (.exec session cmd) ]
     (log-output (.getInputStream cmd))))

; (execute "ping -c 1 google.com" {:host "ec2-54-246-22-16.eu-west-1.compute.amazonaws.com" :user "ubuntu"})
; (execute "ping -c 1 google.com" {:host "localhost" :user "ronen"})

(defn apply-last [m]
  (if-let [[k v] (first (filter (fn [[k v]] (:last (meta v))) m))]
    (assoc m k (with-meta v {:depends (into #{} (remove #(= % k) (keys m)))}))
    m 
    ))

(defn deps-graph [m]
  "tasks graph to depdency graph"
   (reduce (fn [r [k v]] 
     (apply merge-with clojure.set/union r 
       (map (fn [d] {d #{k}}) (:depends (meta v))))) {} m))

(defn sorted-deps [tasks]
  (kahn-sort (deps-graph (apply-last tasks))))

(defn step [steps parent]
  (reduce (fn [r [k v]] (conj r (list 'debug k) (list 'ssh-execute (list '<< v) `~'remote))) (list parent) steps) )

(defn generate-tasks [tasks]
  (reduce 
    (fn [r parent] (apply conj r (step (tasks parent) (list 'debug parent)))) '() (sorted-deps tasks)))

(defmacro run [bindings tasks]
  `(let ~bindings
    ~@(generate-tasks tasks)        
    ))

(macroexpand-1
  '(run [remote {:host "ec2-54-246-22-16.eu-west-1.compute.amazonaws.com" :user "ubuntu"} 
         url "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz" 
         path "redis-sandbox-0.3.4" 
         ] 
    {:download {
      :wget :cd "/tmp" "wget ~{url}" :tar "tar -xvzf redis-sandbox-0.3.4.tar.gz"
     }
     :puppet ^{:depends #{:download}} {
        :cd "cd /tmp/~{path}" :run "sudo ./scripts/run"
     } 
     
   }))

