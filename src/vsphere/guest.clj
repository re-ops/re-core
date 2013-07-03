(ns vsphere.guest
  (:import 
    com.vmware.vim25.NamePasswordAuthentication
    com.vmware.vim25.GuestProgramSpec
    com.vmware.vim25.GuestFileAttributes) 
  (:require 
    [clj-http.client :as client])
  (:use 
    [clojure.java.io :only (copy file reader)]
    [vsphere.vijava :only (with-service tools-installed? service find-vm)]))

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
  "uploads a file into a guest system" 
  [src dst hostname auth]
  {:pre [(tools-installed? hostname)]}
  (with-service 
    (let [file (slurp src) 
          url (.initiateFileTransferToGuest (manager hostname :file) (npa auth) dst (GuestFileAttributes.) (.length file) true)]
      (client/put url {:body file :insecure? true}))))

(defn download-file  
  "download a file from a guest system" 
  [src dest hostname auth]
  {:pre [(tools-installed? hostname)]}
  (with-service 
    (let[{:keys [url]} (bean (.initiateFileTransferFromGuest (manager hostname :file) (npa auth) src))]
      (copy (:body (client/get url {:as :stream :insecure? true}))  (file dest) ))))

(defn guest-run  
  "Runs a remote command using out of band guest access "
  [hostname cmd args auth]
  {:pre [(tools-installed? hostname)]}
  (with-service
    (let [m (manager hostname :proc) spec (doto (GuestProgramSpec.) (.setProgramPath cmd) (.setArguments args))]
      (.startProgramInGuest m  (npa auth) spec))))

(defn set-ip 
   "set guest static ip" 
   [ip hostname auth]

  )
(comment
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"})
  (guest-run "foo" "/usr/bin/touch" "/tmp/foo" {:user "ronen" :pass "foobar"}) 
  (upload-file "project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (download-file "/tmp/project.clj" "/tmp/project.clj" "foo" {:user "ronen" :pass "foobar"}) 
  (tools-installed? "foo")) 
