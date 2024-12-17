(ns re-mote.ssh.nebula
  "Running remote commands over nebula ssh support, available commends include
  
    change-remote - Changes the remote address used in the tunnel for the provided vpn ip
    close-tunnel - Closes a tunnel for the provided vpn ip
    create-tunnel - Creates a tunnel for the provided vpn ip and address
    device-info - Prints information about the network device.
    help - prints available commands or help <command> for specific usage info
    list-hostmap - List all known previously connected hosts
    list-lighthouse-addrmap - List all lighthouse map entries
    list-pending-hostmap - List all handshaking hosts
    log-format - Gets or sets the current log format
    log-level - Gets or sets the current log level
    mutex-profile-fraction - Gets or sets runtime.SetMutexProfileFraction
    print-cert - Prints the current certificate being used or the certificate for the provided vpn ip
    print-relays - Prints json details about all relay info
    print-tunnel - Prints json details about a tunnel for the provided vpn ip
    query-lighthouse - Query the lighthouses for the provided vpn ip
    reload - Reloads configuration from disk, same as sending HUP to the process
    save-heap-profile - Saves a heap profile to the provided path, ex: `heap-profile.pb.gz`
    save-mutex-profile - Saves a mutex profile to the provided path, ex: `mutex-profile.pb.gz`
    start-cpu-profile - Starts a cpu profile and write output to the provided file, ex: `cpu-profile.pb.gz`
    stop-cpu-profile - Stops a cpu profile and writes output to the previously provided file
    version - Prints the currently running version of nebula"
  (:require
   [cheshire.core :refer (parse-string)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)])
  (:import [com.google.json JsonSanitizer]))

(refer-base)
(refer-out)

(def commands #{:change-remote :close-tunnel :create-tunnel :device-info :help :list-hostmap
                :list-lighthouse-addrmap :list-pending-hostmap :log-format :log-level
                :mutex-profile-fraction :print-cert :print-relays :print-tunnel
                :query-lighthouse :reload :save-heap-profile :save-mutex-profile
                :start-cpu-profile :stop-cpu-profile :version})

(defn parse
  [s]
  (parse-string (JsonSanitizer/sanitize s) true))

(defn into-m
  "Convert nebula output into json"
  [[_ {:keys [success]}]]
  (into {} (map (juxt :host (comp parse :out :result)) success)))

(defn run-nebula
  ([hs cmd]
   {:pre [(contains? commands cmd)]}
   (run-nebula hs cmd []))
  ([hs cmd args]
   (if (empty? args)
     (run> (exec hs (name cmd)) | (pretty (name cmd)))
     (run> (exec hs (str (name cmd) " " (clojure.string/join " " args))) | (pretty (name cmd))))))

(defn list-hostmap [hs]
  (into-m (run-nebula hs :list-hostmap ["-json"])))

(defn list-pending-hostmap [hs]
  (into-m (run-nebula hs :list-pending-hostmap ["-json"])))

(defn list-lighthouse-addrmap [hs]
  (into-m (run-nebula hs :list-lighthouse-addrmap ["-json"])))

(defn device-info [hs]
  (into-m (run-nebula hs :device-info ["-json"])))

(defn query-lighthouse [hs ip]
  (into-m (run-nebula hs :query-lighthouse [ip "-json"])))

(defn help [hs cmd]
  (run-nebula hs :help [cmd]))
