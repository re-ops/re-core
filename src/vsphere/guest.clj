(ns vsphere.guest
  (:import 
    com.vmware.vim25.NamePasswordAuthentication
    com.vmware.vim25.GuestProgramSpec
    com.vmware.vim25.GuestFileAttributes) 
  (:require 
    [clj-http.client :as client])
  (:use 
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
    log - true means logging will take place
    wait - how much to wait before logging output
    uuid - the log file uuid
  "
  [hostname cmd args auth {:keys [uuid log wait]}]
  {:pre [(tools-installed? hostname)]}
  (with-service
    (let [m (manager hostname :proc) 
          args* (if log (<< "~{args} >> /tmp/run-~{uuid}.log") args) 
          spec (doto (GuestProgramSpec.) (.setProgramPath cmd) (.setArguments args*)) ]
      (.startProgramInGuest m (npa auth) spec))) 
    (when log (Thread/sleep wait) 
         (debug (slurp (download-file (<< "/tmp/run-~{uuid}.log") hostname auth)))))


(defn set-ip 
  "set guest static ip" 
  [hostname auth config]
  (let [uuid (gen-uuid) tmp-file (<< "/tmp/intrefaces_~{uuid}")]
    (upload-file (render-resource "static-ip.mustache" config) tmp-file  hostname auth)  
    (guest-run hostname "/bin/rm" (<< " -v ~{tmp-file}") auth {:uuid uuid :log true :wait 1000})))

(comment
  (set-ip "foo" {:user "ronen" :pass "foobar"} 
          {:ip "192.168.5.6" :mask "255.255.255.0" :network "192.168.20.0" :gateway "192.168.20.254" :search "local" :names ["192.168.5.0"]}) 
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"})
  (guest-run "foo" "/usr/bin/touch" "/tmp/foo" {:user "ronen" :pass "foobar"} {:uuid (gen-uuid) :log true :wait 1000}) 
  (upload-file "project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (tools-installed? "foo")) 
