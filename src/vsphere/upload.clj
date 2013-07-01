(ns vsphere.upload
  (:import 
    com.vmware.vim25.NamePasswordAuthentication
    com.vmware.vim25.GuestFileAttributes) 
  (:require 
    [clj-http.client :as client])
  (:use 
    [vsphere.vijava :only (with-service tools-installed? service find-vm)]))

(defn- npa
   "converts auth map to new pass auth" 
   [{:keys [user pass] }]
  (doto (NamePasswordAuthentication.) (.setUsername user) (.setPassword pass)))

(defn manager 
   "file manage of vm" 
   [hostname]
   (.getFileManager (.getGuestOperationsManager service) (find-vm hostname)))

(defn upload-file  
   "uploads a file into a guest system" 
   [src dst hostname auth]
  {:pre [(tools-installed? hostname)]}
  (with-service 
    (let [file (slurp src) 
          url (.initiateFileTransferToGuest (manager hostname) (npa auth) dst (GuestFileAttributes.) (.length file) true)]
      (client/put url {:body file :insecure? true}) 
     )))

;; (upload-file "project.clj" "/tmp/project.clj" "bar" {:user "ronen" :pass "foobar"})
