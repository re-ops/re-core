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
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]))

(refer-base)
(refer-out)

(def commands #{:change-remote :close-tunnel :create-tunnel :device-info :help :list-hostmap
                :list-lighthouse-addrmap :list-pending-hostmap :log-format :log-level
                :mutex-profile-fraction :print-cert :print-relays :print-tunnel
                :query-lighthouse :reload :save-heap-profile :save-mutex-profile
                :start-cpu-profile :stop-cpu-profile :version})

(defn run-nebula
  ([hs cmd] 
   {:pre [(contains? commands cmd)]}
   (run-nebula hs cmd []))
  ([hs cmd args]
   (if (empty? args)
     (run> (exec hs (name cmd)) | (pretty (name cmd)))
     (run> (exec hs (str (name cmd) " " (clojure.string/join " " args))) | (pretty (name cmd))))))
