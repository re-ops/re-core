(ns kvm.connection
  (:require 
    [taoensso.timbre :as timbre]
    [clojure.core.strint :refer (<<)]
    [kvm.common :refer (connect)]))

(timbre/refer-timbre)

(declare ^:dynamic *libvirt-connection*)

(defn connection [{:keys [host user port]}]
  (connect (<< "qemu+ssh://~{user}@~{host}:~{port}/system")))

(defn c [] *libvirt-connection*)

(defmacro with-connection [& body]
  `(binding [*libvirt-connection* (connection ~'node)]
     (try
       (do ~@body)
       (finally
         (debug "status code for close libvirt connection" (.close *libvirt-connection*))))))
 
