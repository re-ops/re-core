(comment
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns kvm.networking
  (:require
    [celestial.provider :refer (wait-for)]
    [slingshot.slingshot :refer [throw+]]
    [taoensso.timbre :as timbre]
    [clojure.core.strint :refer (<<)]
    [supernal.sshj :refer (execute get-log collect-log)]
    [celestial.common :refer [gen-uuid]]
    [clojure.java.shell :refer [sh]]
    [clojure.data.zip.xml :as zx]
    [kvm.common :refer (connect domain-zip)]))


(timbre/refer-timbre)

(defn macs [c id]
  (let [root (domain-zip c id)]
    (map vector
      (zx/xml-> root :devices :interface :source (zx/attr :bridge))
      (zx/xml-> root :devices :interface :mac (zx/attr :address)))))

(defn nat-ip
   [c id node]
   (let [[nic mac] (first (macs c id)) uuid (gen-uuid)]
     (execute (<< "arp -i ~{nic}") node :out-fn (collect-log uuid))
     (when-let [line (first (filter #(.contains % mac) (get-log uuid)))]
       (first (.split line "\\s" ))
       )))

(defn wait-for-nat [c id node timeout]
  "Waiting for nat cache to update"
  (wait-for {:timeout timeout} #(not (nil? (nat-ip c id node)))
    {:type ::kvm:networking :timeout timeout}
      "Timed out on waiting for arp cache to update"))

(def ignore-authenticity "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no")

(defn inet-line
   [lines]
   (first (filter #(.contains % "inet addr") lines)))

(defn public-ip
  [c user node id & {:keys [public-nic] :or {public-nic "eth1"}}]
   (wait-for-nat c id node [5 :minute])
   (let [uuid (gen-uuid) nat (nat-ip c id node)
         cmd (<< "ssh ~{ignore-authenticity} ~{user}@~{nat} -C 'ifconfig ~{public-nic}'")]
     (execute cmd node :out-fn (collect-log uuid))
     (if-let [ip (second (re-matches #".*addr\:(\d+\.\d+\.\d+\.\d+).*" (inet-line (get-log uuid))))]
        ip
        (throw+ {:type ::kvm:networking} "Failed to grab domain public IP")
        )))

;; (def connection (connect "qemu+ssh://ronen@localhost/system"))
;; (public-ip connection {:host "localhost" :user "ronen"} "ubuntu-15.04")
