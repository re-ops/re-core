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
  (:require [celestial.persistency :as p])
  (:use 
    [clojure.core.strint :only (<<)]
    [supernal.sshj :only (execute)]))

(defn- ignore-code [s]
  (with-meta s (merge (meta s) {:ignore-code true})))

(def restart 
  "sudo service dnsmasq stop && sudo service dnsmasq start")


(defn hostline [domain {:keys [ip hostname] :as machine}]
  (<< "~{ip} ~{hostname} ~{hostname}.~(get machine :domain domain)"))

(defn add-host 
  "Will add host to hosts file only if missing, 
   note that we p/get-system since the provider might updated the system during create."
  [{:keys [dnsmasq user domain system-id]}]
  (let [remote {:host dnsmasq :user user} line (hostline domain (:machine (p/get-system system-id)))]
    (execute 
      (<< "grep -q '~{line}' /etc/hosts || (echo ~{line} | sudo tee -a /etc/hosts >> /dev/null)") remote)
    (execute restart remote)))


(defn remove-host 
  "Removes host, 
   here we use the original machine since the last step in destroy is clearing the system" 
  [{:keys [dnsmasq user domain machine]}]
  (let [remote {:host dnsmasq :user user} line (hostline domain machine)]
    (execute (<< "sudo sed -ie \"\\|^~{line}\\$|d\" /etc/hosts") remote) 
    (execute restart remote)))

(def actions {:reload {:success add-host} :destroy {:success remove-host}})

(defn update-dns [{:keys [event workflow] :as args}]
  ((get-in actions [workflow event] identity) args))

