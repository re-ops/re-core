(ns physical.provider
  "Physical machine management,
   * start/creation/deletion are not supported.
   * stop will run remote stop command via ssh
   * status will use ssh to try and see if the machine is running"
  (:refer-clojure :exclude [sync])
  (:require
   [physical.validations :refer (validate-provider)]
   [re-core.provider :refer (wait-for-ssh mappings)]
   [re-share.core :refer (wait-for)]
   [re-core.common :refer (bash-)]
   [clojure.core.strint :refer (<<)]
   [re-mote.ssh.transport :refer (ssh-up? execute)]
   [re-mote.repl :refer (host-scan port-scan deploy)]
   [re-mote.zero.facts :refer (os-info)]
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

(defn ssh-able
  [hosts ports]
  (filter
   (fn [host]
     (some
      (fn [{:keys [portid]}] (= portid "22")) (ports host))) hosts))

(defn result [[_ m]]
  (-> m :success first :result))

(defrecord Scanner [opts]
  Sync
  (sync [this]
    (let [{:keys [pivot network re-gent]} opts
          addresses (-> (host-scan pivot network) result physical-hosts)
          ports (result (port-scan pivot network))
          deployed (deploy (Hosts. (.auth pivot) (ssh-able (mapcat keys addresses) ports)) re-gent)]
      [])))

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
