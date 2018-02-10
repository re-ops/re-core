(ns physical.provider
  "Physical machine management,
   * creation is not supported maybe pxe boot in future?
   * deletion is not supported.
   * start will use wake on lan)
   * stop will run remote stop command via ssh
   * status will use ssh to try and see if the machine is running
    "
  (:require
   [physical.validations :refer (validate-provider)]
   [re-core.provider :refer (wait-for-ssh mappings)]
   [re-share.core :refer (wait-for)]
   [re-core.common :refer (bash-)]
   [clojure.core.strint :refer (<<)]
   [re-mote.ssh.transport :refer (ssh-up? execute)]
   [re-core.core :refer (Vm)]
   [physical.wol :refer (wol)]
   [re-core.model :refer (translate vconstruct)]
   [taoensso.timbre :refer (refer-timbre)]))

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

(defmethod translate :physical
  [{:keys [physical machine]}]
  [(mappings  (select-keys machine [:hostname :ip :user]) {:hostname :host})
   (select-keys physical [:mac :broadcast])])

(defn validate [[remote interface :as args]]
  (validate-provider remote interface) args)

(defmethod vconstruct :physical [spec]
  (apply ->Machine  (validate (translate spec))))
