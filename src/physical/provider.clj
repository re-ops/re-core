(ns physical.provider
  "Physical machine management,
   * start/creation/deletion are not supported.
   * stop will run remote stop command via ssh
   * status will use ssh to try and see if the machine is running"
  (:refer-clojure :exclude [sync])
  (:require
   [physical.validations :refer (validate-provider)]
   [re-core.provider :refer (wait-for-ssh mappings)]
   [re-share.wait :refer (wait-for)]
   [re-core.common :refer (bash-)]
   [re-mote.ssh.transport :refer (ssh-up? execute)]
   [re-mote.repl :refer (host-scan port-scan deploy)]
   [re-core.core :refer (Sync Vm)]
   [physical.wol :refer (wol)]
   [re-core.model :refer (translate vconstruct sconstruct)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defrecord Machine [remote interface]
  Vm
  (create [this]
    (throw (ex-info "cannot create a phaysical machine" {:type ::not-supported})))

  (delete [this]
    (throw (ex-info "cannot delete a phaysical machine" {:type ::not-supported})))

  (start [this]
    (wol interface)
    (wait-for-ssh (remote :host) (remote :user) [10 :minute]))

  (stop [this]
    (execute (bash- ("sudo" "shutdown" "0" "-P")) remote)
    (wait-for {:timeout [5 :minute]}
              (fn []
                (try
                  (not (ssh-up? remote))
                  (catch java.net.NoRouteToHostException t true)))
              "Timed out while waiting for machine to shutdown"))

  (status [this]
    (try
      (if (ssh-up? remote) "running" "Nan")
      (catch Throwable t "Nan")))

  (ip [this]
    (remote :ip)))

(defn physical-hosts
  "Filter physical hosts from the scan result"
  [result]
  (filter
   (fn [m]
     (not (some (fn [{:keys [vendor]}] (= vendor "QEMU virtual NIC")) (first (vals m))))) result))

(defn ssh-able [addresses ports]
  (filter
   (fn [address]
     (let [host (first (keys address))]
       (some
        (fn [{:keys [portid]}] (= portid "22")) (ports host)))) addresses))

(defn result [[_ m]]
  (-> m :success first :result))

(defn find-address [type addresses]
  (first (filter (fn [{:keys [addrtype]}] (= addrtype type)) addresses)))

(defn base-system [host user ip domain]
  {:machine {:hostname host :user user :ip (ip :addr) :domain domain :os :unknown-os}
   :physical {}
   :type :unknown-type})

(defn into-system
  "Convert scan result into a system"
  [user [fqdn addresses]]
  (let [[host domain] (clojure.string/split fqdn '#"\.")
        mac (find-address "mac" addresses)
        ip (find-address "ipv4" addresses)
        machine (base-system host user ip domain)]
    (if-not mac
      machine
      (merge machine {:physical {:mac (mac :addr) :vendor (mac :vendor)}}))))

(defrecord Scanner [opts]
  Sync
  (sync [this]
    (let [{:keys [pivot network user]} opts
          addresses (-> (host-scan pivot network) result physical-hosts)
          ports (result (port-scan pivot network))
          hosts (apply merge (ssh-able addresses ports))]
      (map (partial into-system user) hosts))))

(defmethod sconstruct :physical [_ opts]
  (Scanner. opts))

(defmethod translate :physical
  [{:keys [physical machine]}]
  [(mappings  (select-keys machine [:hostname :ip :user]) {:hostname :host})
   (select-keys physical [:mac :broadcast])])

(defn validate [[remote interface :as args]]
  (validate-provider remote interface) args)

(defmethod vconstruct :physical [spec]
  (apply ->Machine  (validate (translate spec))))
