(ns digital.provider
  "Digital ocean provider"
  (:require
   [digital.validations :refer (provider-validation)]
   [clojure.core.strint :refer (<<)]
   [re-core.model :refer (translate vconstruct hypervisor*)]
   [re-core.provider :refer (mappings transform selections os->template wait-for-ssh wait-for-start wait-for-stop)]
   [re-share.wait :refer (wait-for)]
   [re-core.persistency.systems :as s :refer (system-val)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.core :refer (Vm)]
   [digitalocean.v2.core :as d]))

(refer-timbre)

(defn run-action [type* id]
  (let [post-action (d/generic :post (<< "droplets/~{id}/actions"))]
    (post-action (hypervisor* :digital-ocean :token) nil  {:type (name type*)})))

(defn get-droplet
  "get droplet using token"
  [id]
  (d/get-droplet (hypervisor* :digital-ocean :token) id))

(defn get-ip [id]
  (get-in (get-droplet id) [:droplet :networks :v4 0 :ip_address]))

(defn wait-for-ip
  "Wait for an ip to be avilable"
  [id timeout]
  (wait-for {:timeout timeout} #(not (nil? (get-ip id)))
            "Timed out on waiting for ip to be available"))

(defmacro with-id [& body]
  `(if-let [~'id (system-val ~'spec [:digital-ocean :id])]
     (do ~@body)
     (throw (ex-info "Droplet id not found" {:id 'id}))))

(defrecord Droplet [token drp spec]
  Vm
  (create [this]
    (let [{:keys [droplet message] :as result} (d/create-droplet token nil drp) {:keys [id]} droplet]
      (when-not droplet
        (throw (ex-info message {:droplet droplet})))
      (wait-for-ip id [5 :minute])
      (let [ip (get-ip id)]
        (s/partial (spec :system-id) {:machine {:ip ip} :digital-ocean {:id id}}))
      this))

  (delete [this]
    (with-id
      (d/delete-droplet token id)))

  (start [this]
    (with-id
      (let [ip (get-ip id)]
        (run-action "power_on" id)
        (wait-for-start this [5 :minute] ::digital:start-failed)
        (wait-for-ssh ip (:user spec) [5 :minute]))))

  (stop [this]
    (with-id
      (run-action "power_off" id)
      (wait-for-stop this [5 :minute])))

  (status [this]
    (if-let [id (system-val spec [:digital-ocean :id])]
      (let [status-map {"active" "running" "off" "stop"}
            droplet-status (get-in (get-droplet id) [:droplet :status])]
        (or (status-map droplet-status) droplet-status))
      (do (debug "id not found, instance not created") false)))
  (ip [this]
    (with-id (get-ip id))))

(defn machine-ts
  "Construcuting machine transformations"
  [{:keys [domain]}]
  {:name (fn [host] (<< "~{host}.~{domain}")) :image (fn [os] (:image ((os->template :digital-ocean) os)))})

(def drop-ks [:name :region :size :image :ssh_keys])

(defmethod translate :digital-ocean [{:keys [machine digital-ocean system-id] :as spec}]
  (-> (merge machine digital-ocean {:system-id system-id})
      (mappings {:os :image :hostname :name})
      (transform (machine-ts machine))
      (assoc :ssh_keys [(hypervisor* :digital-ocean :ssh-key)])
      (selections [drop-ks [:system-id :user]])))

(defmethod vconstruct :digital-ocean [{:keys [digital-ocean machine] :as spec}]
  (let [[translated ext] (translate spec)]
    (provider-validation translated)
    (->Droplet (hypervisor* :digital-ocean :token) translated ext)))

