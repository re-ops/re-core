(ns aws.volumes
  (:require
   [re-core.provider :refer (wait-for)]
   [amazonica.aws.ec2 :as ec2]
   [re-core.persistency.systems :as s]
   [aws.common :refer (with-ctx instance-desc creds image-id)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn image-desc [endpoint ami & ks]
  (->
   (ec2/describe-images (assoc (creds) :endpoint endpoint)
                        :image-ids [ami]) :images first (apply ks)))

(defn wait-for-attach [endpoint instance-id timeout]
  (wait-for {:timeout timeout}
            #(= "attached" (instance-desc endpoint instance-id :block-device-mappings 0 :ebs :status))
            {:type ::aws:ebs-attach-failed} "Failed to wait for ebs root device attach"))

(defn volume-desc [endpoint volume-id & ks]
  (->
   (with-ctx ec2/describe-volumes {:volume-ids [volume-id]})
   :volumes first (get-in ks)))

(defn create-volume
  "Create a volume"
  [{:keys [device size volume-type iops]} zone endpoint]
  (get-in
   (with-ctx ec2/create-volume
     (cond-> {:size size :availability-zone zone :volume-type volume-type}
       iops (assoc :iops iops))) [:volume :volume-id]))

(defn handle-volumes
  "attaches and waits for ebs volumes"
  [{:keys [aws machine] :as spec} endpoint instance-id]
  (when (= (image-desc endpoint (image-id machine) :root-device-type) "ebs")
    (wait-for-attach endpoint instance-id [10 :minute]))
  (let [zone (instance-desc endpoint instance-id :placement :availability-zone)]
    (doseq [{:keys [device] :as vol} (aws :volumes)
            :let [volume-id (create-volume vol zone endpoint)]]
      (wait-for {:timeout [10 :minute]}
                #(= "available" (volume-desc endpoint volume-id :state))
                {:type ::aws:ebs-volume-availability} "Failed to wait for ebs volume to become available")
      (with-ctx ec2/attach-volume
        {:volume-id volume-id :instance-id instance-id :device device})
      (wait-for {:timeout [10 :minute]}
                #(= "attached" (volume-desc endpoint volume-id :attachments 0 :state))
                {:type ::aws:ebs-volume-attach-failed} "Failed to wait for ebs volume device attach")
      (debug "attached volume" volume-id "owened by" instance-id))))

(defn clear?
  "is this ebs clearable"
  [device-name system-id]
  (let [{:keys [volumes]} ((s/get-system system-id) :aws)]
    (if-let [e (first (filter (fn [{:keys [device]}] (= device device-name)) volumes))]
      (e :clear)
      false)))

(defn delete-volumes
  "Clear instance volumes"
  [endpoint instance-id system-id]
  (doseq [{:keys [ebs device-name]} (-> (instance-desc endpoint instance-id) :block-device-mappings rest)
          :let [{:keys [volume-id]} ebs]]
    (when (clear? device-name system-id)
      (debug "deleting volume" volume-id)
      (with-ctx ec2/detach-volume {:volume-id volume-id})
      (wait-for {:timeout [10 :minute]}
                #(= "available" (volume-desc endpoint volume-id :state))
                {:type ::aws:ebs-volume-availability} "Failed to wait for ebs volume to become available")
      (with-ctx ec2/delete-volume {:volume-id volume-id}))))
