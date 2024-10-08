(ns re-mote.repl
  "Main remote workflow functions of Re-mote, it includes functions for performing a range of operations from updating packages to running an Nmap scan and collecting metrics.
   For more info check https://re-ops.github.io/re-ops/"
  (:refer-clojure :exclude  [update])
  (:require
   [me.raynes.fs :as fs]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.cog :refer (refer-cog)]
   [re-mote.validate :refer (check-entropy check-jce)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.persist.es :refer (refer-es-persist)]
   [re-mote.repl.zero.desktop :refer (refer-desktop)]
   [re-mote.repl.zfs :refer (refer-zfs)]
   [re-mote.repl.stress :refer (refer-stress)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.publish :refer (refer-publish)]
   [re-mote.repl.spec :refer (refer-spec)]
   [re-mote.repl.octo :refer (refer-octo)]
   [re-mote.zero.restic :refer (refer-restic)]
   [re-mote.zero.devices :refer (refer-devices)]
   [re-mote.zero.stats :refer (refer-stats)]
   [re-mote.zero.certs :refer (refer-certs)]
   [re-mote.zero.scp :refer (refer-zero-scp)]
   [re-mote.zero.security :refer (refer-security)]
   [re-mote.zero.sensors :refer (refer-zero-sensors)]
   [re-mote.repl.re-gent :refer (refer-regent)]
   [re-mote.zero.facts :refer (refer-facts)]
   [re-mote.zero.osquery :refer (refer-osquery)]
   [re-mote.zero.process :refer (refer-process)]
   [re-mote.zero.git :refer (refer-git)]
   [re-mote.zero.pkg :refer (refer-zero-pkg)]
   [re-mote.repl.pkg :refer (refer-pkg)]
   [re-mote.log :refer (setup-logging)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-facts)
(refer-process)
(refer-base)
(refer-out)
(refer-stats)
(refer-certs)
(refer-devices)
(refer-security)
(refer-zero-sensors)
(refer-pkg)
(refer-zero-scp)
(refer-zero-pkg)
(refer-spec)
(refer-zfs)
(refer-publish)
(refer-octo)
(refer-restic)
(refer-regent)
(refer-git)
(refer-es-persist)
(refer-desktop)
(refer-stress)
(refer-osquery)
(refer-cog)

(defn setup
  "Setup Re-mote environment as a part of the Reload workflow"
  []
  (check-entropy 200)
  (check-jce)
  (setup-logging))

(defn single
  "Create a single hosts instance"
  [h & m]
  (Hosts. (merge {:user "re-ops"} (first m)) [h]))

; Security

(defn ^{:category :security} ports-persist
  "Scan for open ports and persist into ES:
     (ports-persist hs \"192.168.1.0/24\")"
  [hs network]
  (run (open-ports hs "-T5" network) | (enrich "nmap-scan") | (split by-hosts) | (split nested) | (persist)))

(defn ^{:category :security} port-scan
  "Scan for running open ports on the network:
     (port-scan hs \"192.168.1.0/24\")"
  [hs network]
  (run> (open-ports hs "-T5" network) | (pretty "ports scan")))

(defn ^{:category :security} host-scan
  "Scan for running hosts on the network:
     (host-scan hs \"192.168.1.0/24\")"
  [hs network]
  (run> (security/hosts hs "-sP" network) | (pretty "hosts scan")))

(defn ^{:category :security} inactive-firewall
  "Find hosts with inactive firewall:
     (inactive-firewall hs)"
  [hs]
  (run> (rules hs) | (pick (fn [success failure hosts] (mapv :host (failure 1))))))

(defn ^{:category :security} ssh-sessions
  "List active ssh sessions per host:
     (ssh-sessions hs)"
  [hs]
  (run> (security/ssh-sessions hs) | (enrich "ssh-sessions") | (persist)))

; Persistent stats

(defn ^{:category :stats} du-persist
  "Collect disk usage with persist (metrics collection):
     (du-persist hs)"
  [hs]
  (run> (du hs) | (enrich "du") | (persist) | (riemann)))

(defn ^{:category :stats} cpu-persist
  "Collect CPU and idle usage with persistence (metrics collection):
     (cpu-persist hs)
  "
  [hs]
  (run> (cpu hs) | (enrich "cpu") | (persist) | (riemann)))

(defn ^{:category :stats} entropy-persist
  "Collect Available entropy with persistence (metrics collection):
     (entropy-persist hs)
  "
  [hs]
  (run> (entropy hs) | (enrich "entropy") | (persist) | (riemann)))

(defn ^{:category :stats} ram-persist
  "Collect free and used RAM usage with persistence (metrics collection):
     (ram-persist hs)
  "
  [hs]
  (run> (free hs) | (enrich "free") | (persist) | (riemann)))

(defn ^{:category :stats} net-persist
  "Collect networking in/out kbps and persist (metric collection):
     (net-persist hs)
  "
  [hs]
  (run> (net hs) | (enrich "net") | (persist) | (riemann)))

(defn ^{:category :stats} sensor-persist
  "Collect Sensor data (using lm-sensors) and persist (metric collection):
     (sensor-persist hs)
   "
  [hs]
  (run> (zsens/sensor hs) | (enrich "sensor") | (persist) | (riemann)))

(defn ^{:category :stats} load-persist
  "Read average load and persist is (metrics collection):
     (load-persist hs)
   "
  [hs]
  (run> (load-avg hs) | (enrich "load") | (persist) | (riemann)))

; Device tracking

(defn ^{:category :stats} usb-devices
  "Read average load and persist is (metrics collection):
     (load-persist hs)
   "
  [hs]
  (run> (usb hs) | (enrich "usb") | (riemann)))

; Packaging

(defn ^{:category :packaging} update
  "Update the package repository of the hosts:
     (update hs)
  "
  [hs]
  (run (zpkg/update hs) | (notify "package update") | (enrich "update")))

(defn ^{:category :packaging} upgrade
  "Run package update followed by an upgrade on hosts that were updated successfully:
     (upgrade hs)
    "
  [hs]
  (run (zpkg/update hs) | (pick successful) |  (zpkg/upgrade) | (pretty "upgrade") | (notify "package upgrade") | (enrich "upgrade")))

(defn ^{:category :packaging} install
  "Install a package on hosts:
     (install hs \"openjdk8-jre\")
  "
  [hs pkg]
  (run (zpkg/install hs pkg) | (downgrade pkg/install [pkg]) | (pretty "package install")))

; Re-cog

(defn ^{:category :re-cog} provision
  "Provision hosts copying local file resources and then applying  a provisioning plan:
     (provision hs into-hostnames {:src src :plan p :args args})

   * into-hostnames - A function that maps result Hosts ips into hostnames
  "
  [hs into-hostnames {:keys [src plan args]}]
  {:pre [src plan]}
  (let [dest (<< "/tmp/~(fs/base-name src)/")]
    (assert (clojure.string/ends-with? src "/"))
    (run> (rm hs dest "-rf") | (sync- src dest) | (pick successful) | (convert into-hostnames) | (run-plan plan args))))

; Re-gent

(defn ^{:category :re-gent} deploy
  "Deploy re-gent and setup .curve remotely:
     (deploy hs \"re-gent/target/re-gent\")"
  [{:keys [auth] :as hs} bin]
  (let [{:keys [user]} auth home (<< "/home/~{user}") dest (<< "~{home}/.curve")]
    (run (mkdir hs dest "-p") | (scp-into ".curve/server-public.key" dest) | (pretty "curve copy"))
    (run (kill-agent hs) | (pretty "kill agent"))
    (run> (scp-into hs bin home) | (pick successful) | (start-agent home) | (pretty "scp"))))

(defn ^{:category :re-gent} kill
  "Kill a re-gent process on all of the hosts:
     (kill hs)"
  [hs]
  (run (kill-agent hs) | (pretty "kill agent")))

(defn ^{:category :re-gent} unregister
  "Unregister hosts from our ZMQ registry, (doesn't close any connection):
     (kill hs)"
  [hs]
  (run (unregister-hosts hs) | (pretty "unregister hosts")))

(defn ^{:category :re-gent} launch
  "Start a re-gent process on hosts:
     (launch hs)
  "
  [{:keys [auth] :as hs}]
  (let [{:keys [user]} auth home (<< "/home/~{user}")]
    (run (start-agent hs home) | (pretty "launch agent"))))

(defn pull
  "Pull latest git repository changes:
     (pull hs {:repo \"re-core\" :branch \"master\" :remote \"git://github.com/re-ops/re-mote.git\"})"
  [hs {:keys [repo remote branch]}]
  (run (git/pull hs repo remote branch) | (pretty "git pull")))

; Basic tasks

(defn copy-to
  "Copy a local file into remote hosts:
    (copy-to (hosts (matching  \"foo\") :ip) \"/tmp/1\" \"/home/re-ops/bar\")
  "
  [hs src dest]
  (run (scp-into hs src dest) | (pretty "file copied")))

(defn copy-from
  "Copy a file from remote hosts locally:
    (copy-from (hosts (matching  \"foo\") :ip) \"/home/re-ops/bar\" \"/tmp/1\")

   Copy a file from remote hosts to a set of remote hosts:
    (copy-from (hosts (matching  \"foo\") :ip) \"/home/re-ops/bar\" (hosts (matching  \"bar\") :hostname) \"/tmp/1\")
  "
  ([hs src dest]
   (run (scp-from hs src dest) | (pretty "file downloaded")))
  ([hs-src src hs-dst dest recursive?]
   (run> (z-scp/scp-from hs-dst dest hs-src src recursive?) | (pretty "file downloaded"))))

(defn copy-from-to
  "Copy a file from a single host and then copy it into other set of remote hosts (file distribution)
    (copy-from-to (hosts (matching \"foo\") :ip) (hosts (matching \"foo\") :ip) \"/home/re-ops/bar\" \"/tmp/1\")
   "
  [src-host dst-hosts src dest]
  (let [file (last (clojure.string/split src #"\/"))]
    (run (scp-from src-host src (<< "/tmp/")) | (pretty "file downloaded from host"))
    (run (scp-into dst-hosts (<< "/tmp/~{file}") dest) | (pretty "file uploaded into hosts"))
    (me.raynes.fs/delete (<< "/tmp/~{file}"))))

; Desktop

(defn browse-to
  "Open a browser url:
    (browse-to hs \"github.com\")
  "
  [hs url]
  (run (browse hs url) | (pretty "opened browser")))

(defn open-file
  "Open a file using a remote browser:
     (open-file hs \"/home/foo/bar.pdf\")
   "
  [hs src]
  (let [dest (<< "/tmp/~(fs/base-name src)")
        ext (fs/extension src)]
    (cond
      (#{".pdf" ".html"} ext) (run (scp-into hs src dest) | (browse dest) | (pretty "file opened"))
      (#{".doc" ".docx" ".odt"} ext) (run (scp-into hs src dest) | (writer dest) | (pretty "file opened")))))

; Process management


(defn process-matching
  "Find processes matching target name:
    (process-matching hs \"ssh\"); find all ssh processes
  "
  [hs target]
  (run> (processes hs target) | (pretty "process-matching")))

; Backup

(defn init-backup
  "Initialize a single backup:

    (init-backup (hosts (matching <id>) :hostname) :<key> bs)
  "
  [hs bs k]
  {:pre [(keyword? k) (map? bs)]}
  (run (init hs (bs k) [1 :minute]) | (notify (<< "restic init of ~{k}"))))

(defn run-backups
  ([hs bs]
   (run-backups hs bs [24 :hours]))
  ([hs bs t]
   (doseq [[k b] bs]
     (run> (backup hs b t) | (notify (<< "restic backup of ~{k}"))))))

(defn check-backups
  ([hs bs]
   (check-backups hs bs [2 :hours]))
  ([hs bs t]
   (doseq [[k b] bs]
     (run (check hs b t) | (notify (<< "restic check of ~{k}"))))))

(defn unlock-backups
  ([hs bs]
   (unlock-backups hs bs [1 :minutes]))
  ([hs bs t]
   (doseq [[k b] bs]
     (run (unlock hs b t) | (notify (<< "restic unlocking of ~{k}"))))))

; Certs

(defn ^{:category :security} cert-renew
  "Cert renewal"
  [hs user token]
  (run> (renew hs user token) | (pretty "cert renwal")))

(defn ^{:category :security} setup-domains
  "Cert domains setup"
  [hs domains]
  (run> (set-domains hs domains) | (pretty "domain certs set")))

