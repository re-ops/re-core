(ns proxmox.generators
  "proxomx generated valudes"
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]
    [proxmox.remote :only (prox-get)]
    [celestial.common :only (get* import-logging)]
    [celestial.persistency :only (defgen)]))

(import-logging)

(defgen ct-id)

(defn ct-exists [id]
  (try+ 
    (prox-get (<< "/nodes/proxmox/openvz/~{id}")) true
    (catch [:status 500] e false)))

(defn try-gen
  "Attempts to generate an id"
  []
  (loop [i 5]
    (when (> i 0)
      (let [id (+ 100 (gen-ct-id))]
        (debug id)
        (if-not (ct-exists id)
          id
          (recur (- i 1)))))))

(defn ct-id
  "Generates a container id" 
  [_]
   (if-let [gid (try-gen)]
      gid
     (throw+ {:type :id-gen-error} "Failed to generate id for container")))

