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

(ns hooks.dnsmasq
  "A basic dnsmasq registration api for static addresses using hosts file, 
   expects an Ubuntu and dnsmasq on the other end "
  (:use 
    [clojure.core.strint :only (<<)]
    [supernal.sshj :only (execute)]))

(defn ignore-code [s]
  (with-meta s (merge (meta s) {:ignore-code true})))

(def restart 
  "sudo service dnsmasq stop && sudo service dnsmasq start")

(defn add-host [{:keys [hostname ip dnsmasq]}]
  (execute (<< "echo '~{ip} ~{hostname}' | sudo tee -a /etc/hosts >> /dev/null" ) dnsmasq)
  (execute restart dnsmasq))


(defn remove-host [{:keys [hostname ip dnsmasq]}]
  (execute (<< "sudo sed -ie \"\\|^~{ip} ~{hostname}\\$|d\" /etc/hosts") dnsmasq)
  (execute restart dnsmasq))

; (add-host "foo" "192.168.20.90")
; (remove-host "foo" "192.168.20.90")
