(ns proxmox.generators
  "proxomx generated valudes"
  (:use 
    [puny.core :only (defgen)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]
    [proxmox.remote :only (prox-get)]
    [celestial.common :only (import-logging)]))

(import-logging)

(defgen ct-id)

(defn ct-exists [id node]
  (try+ 
    (prox-get (<< "/nodes/~{node}/openvz/~{id}/status/current"))
    (catch [:status 500] e false)))

(defn qm-exists [id node]
  (try+ 
    (prox-get (<< "/nodes/~{node}/qemu/~{id}/status/current"))
    (catch [:status 500] e false)))

(defn try-gen
  "Attempts to generate an id"
  [node]
  (loop [i 5]
    (when (> i 0)
      (let [id (+ 100 (gen-ct-id))]
        (trace (<< "testing ~{id} availability"))
        (if-not (or (ct-exists id node) (qm-exists id node)) 
          id
          (recur (- i 1)))))))

(defn ct-id
  "Generates a container id for machine on node" 
  [node]
  (fn [_] 
    (if-let [gid (try-gen node)]
      gid
      (throw+ {:type ::id-gen-error} "Failed to generate id for container"))))


