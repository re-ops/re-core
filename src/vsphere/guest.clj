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

(ns vsphere.guest
  (:import 
    com.vmware.vim25.NamePasswordAuthentication
    com.vmware.vim25.GuestProgramSpec
    com.vmware.vim25.GuestFileAttributes) 
  (:require 
    [clj-http.client :as client])
  (:use 
    [celestial.provider :only (wait-for)]
    [celestial.common :only (gen-uuid import-logging)]
    [clostache.parser :only (render-resource)] 
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (copy file reader)]
    [vsphere.vijava :only (with-service tools-installed? service find-vm)]))

(import-logging)

(defn- npa
   "converts auth map to new pass auth" 
   [{:keys [user pass] }]
  (doto (NamePasswordAuthentication.) (.setUsername user) (.setPassword pass)))

(defn- manager 
  "file manage of vm" 
  [hostname t]
  (let [manager (.getGuestOperationsManager service)]
    (case t
      :file (.getFileManager manager (find-vm hostname))
      :proc  (.getProcessManager manager (find-vm hostname))
      )))

(defn upload-file  
  "uploads a file/string into a guest system" 
  [input dst hostname auth]
  {:pre [(tools-installed? hostname)]}
  (with-service 
    (let [url (.initiateFileTransferToGuest (manager hostname :file) (npa auth) dst (GuestFileAttributes.) (.length input) true)]
      (client/put url {:body input :insecure? true}))))

(defn download-file  
  "download a file from a guest system returns a stream" 
  [src hostname auth]
  {:pre [(tools-installed? hostname)]}
  (with-service 
    (let[{:keys [url]} (bean (.initiateFileTransferFromGuest (manager hostname :file) (npa auth) src))]
      (:body (client/get url {:as :stream :insecure? true})))))

(defn guest-run  
  "Runs a remote command using out of band guest access, 
    wait - how much to wait before logging output
    uuid - the log file uuid, if missing no logging will take place"
  [hostname cmd args auth uuid timeout]
  {:pre [(tools-installed? hostname)]}
  (with-service
    (let [m (manager hostname :proc) 
          args* (if uuid (<< "~{args} >> /tmp/run-~{uuid}.log") args) 
          spec (doto (GuestProgramSpec.) (.setProgramPath cmd) (.setArguments args*)) 
          pid (.startProgramInGuest m (npa auth) spec)]
         (wait-for {:timeout timeout :sleep [200 :ms]}
           #(-> (.listProcessesInGuest m (npa auth) (long-array [pid])) first bean :exitCode nil? not) 
           {:type ::vsphere:guest-run-timeout :message (<< "Timed out on running ~{cmd} ~{args} in guest") :timeout timeout} ))))

(defn fetch-log 
   "fetched remote log file by uuid"
   [hostname uuid auth]
  (slurp (download-file (<< "/tmp/run-~{uuid}.log") hostname auth)))

(defn set-ip 
  "set guest static ip" 
  [hostname auth config]
  (let [uuid (gen-uuid) tmp-file (<< "/tmp/intrefaces_~{uuid}")]
    (upload-file (render-resource "static-ip.mustache" config) tmp-file hostname auth)
    (guest-run hostname "/bin/cp" (<< "-v ~{tmp-file} /etc/network/interfaces") auth uuid [2 :seconds])
    (guest-run hostname "/usr/sbin/service" "networking restart" auth uuid [3 :seconds])
    (guest-run hostname "/bin/rm" (<< "-v ~{tmp-file}") auth uuid [2 :seconds])
    (debug (fetch-log hostname uuid auth))))

(comment
  (set-ip "foo" {:user "root" :pass "foobar"} 
     {:ip "192.168.5.91" :mask "255.255.255.0" :network "192.168.5.0" :gateway "192.168.5.1" :search "local" :names ["192.168.5.1"]}) 
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "root" :pass "foobar"})
  (upload-file "project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (tools-installed? "foo")) 
