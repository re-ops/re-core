(ns hooks.dnsmasq
  "DNS registration hook for static addresses using hosts file:
   * expects an Ubuntu and dnsmasq on the other end.
   * Uses an agent to make sure that only a single action will be performed concurrently. "
  (:require
     pallet.stevedore.bash
    [re-core.persistency.systems :as s]
    [taoensso.timbre :refer (refer-timbre)]
    [re-core.common :refer (bash-)])
  (:use
    [clojure.core.strint :only (<<)]
    [re-mote.sshj :only (execute)]))

(refer-timbre)

(defn- ignore-code [s]
  (with-meta s (merge (meta s) {:ignore-code true})))

; using an agent makes sure that only one action will take place at a time
(def hosts (agent "/etc/hosts"))

(def ^:dynamic sudo "sudo")

(defn restart [remote]
   (execute (bash- (chain-and (~sudo "service" "dnsmasq" "stop") (~sudo "service" "dnsmasq" "start"))) remote))

(defn hostline [domain {:keys [ip hostname] :as machine}]
  (<< "~{ip} ~{hostname} ~{hostname}.~(get machine :domain domain)"))

(defn add-host
  "Will add host to hosts file only if missing,
   note that we s/get-system since the provider might updated the system during create."
  [hosts-file {:keys [dnsmasq user domain system-id] :as args}]
  (try
    (let [remote {:host dnsmasq :user user} line (hostline domain (:machine (s/get-system system-id)))
        hosts-file' (str hosts-file) ]
    (execute
       (bash- (chain-or ("grep" "-q" (quoted ~line) ~hosts-file')
           (pipe ("echo" ~line) (~sudo "tee" "-a" ~hosts-file' ">> /dev/null")))) remote)
    (restart remote) hosts-file)
    (catch Throwable t (error t) hosts-file)))

(defn remove-host
  "Removes host,
   here we use the original machine since the last step in destroy is clearing the system"
  [hosts-file {:keys [dnsmasq user domain machine]}]
  (try
    (let [remote {:host dnsmasq :user user} line (hostline domain machine)
        match (<< "\"\\|^~{line}\\$|d\"")]
    (execute (bash- (~sudo "sed" "-ie" ~match ~(str hosts-file))) remote)
    (restart remote) hosts-file)
    (catch Throwable t (error t) hosts-file)
    ))

(def actions {:reload {:success add-host} :create {:success add-host}
              :start {:success add-host} :stop {:success remove-host}
              :destroy {:success remove-host :error remove-host}
              :stage {:success add-host}
              })

(defn update-dns [{:keys [event workflow] :as args}]
  (try
    (when (agent-error hosts) (restart-agent hosts "/etc/hosts"))
     (send-off hosts (get-in actions [workflow event] (fn [hosts-file _] hosts-file)) args)
    (catch Throwable t
      (when (agent-error hosts)
        (error "Agent has errors restarting during catch of:" t)
        (restart-agent hosts "/etc/hosts")))))

