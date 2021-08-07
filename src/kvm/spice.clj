(ns kvm.spice
  "KVM spice connection"
  (:require
   [re-share.core :refer  (gen-uuid)]
   [clojure.core.strint :refer (<<)]
   [clojure.java.shell :refer [sh]]
   [clojure.data.zip.xml :as zx]
   [kvm.common :refer (domain-zip)]
   [me.raynes.fs :refer (temp-file copy)]))

(defn graphics [c domain]
  (let [root (domain-zip c domain)]
    {:port (first (zx/xml-> root :devices :graphics (zx/attr :port)))
     :listen (first (zx/xml-> root :devices :graphics (zx/attr :listen)))}))

(defn manager-view [connection domain]
  (sh "/usr/bin/virt-manager" "--connect" connection "--show-domain-console" domain))
