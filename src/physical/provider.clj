(comment
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

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
    [re-core.provider :refer (wait-for-ssh mappings wait-for)]
    [re-core.common :refer (bash-)]
    [clojure.core.strint :refer (<<)]
    [re-mote.sshj :refer (ssh-up? execute)]
    [re-core.core :refer (Vm)]
    [slingshot.slingshot :refer  [throw+ try+]]
    [physical.wol :refer (wol)]
    [re-core.model :refer (translate vconstruct)]
    [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defrecord Machine [remote interface]
  Vm
  (create [this]
     (throw+ {:type ::not-supported} "cannot create a phaysical machine"))

  (delete [this]
     (throw+ {:type ::not-supported} "cannot delete a phaysical machine"))

  (start [this]
     (wol interface)
     (wait-for-ssh (remote :host) (remote :user) [10 :minute]))

  (stop [this]
     (execute (bash- ("sudo" "shutdown" "0" "-P")) remote)
     (wait-for {:timeout [5 :minute]}
        #(try
           (not (ssh-up? remote))
          (catch java.net.NoRouteToHostException t true))
       {:type ::shutdown-failed} "Timed out while waiting for machine to shutdown"))

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
