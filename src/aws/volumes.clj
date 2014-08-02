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

(ns aws.volumes
  (:require 
    [celestial.common :refer (import-logging )] 
    [celestial.provider :refer (wait-for)] 
    [amazonica.aws.ec2 :as ec2]
    [celestial.persistency.systems :as s]
    [aws.common :refer (with-ctx instance-desc creds image-id)]
    )) 

(import-logging)

(defn image-desc [endpoint ami & ks]
  (-> 
    (ec2/describe-images (assoc (creds) :endpoint endpoint) 
       :image-ids [ami]) :images first (apply ks)
      ))

(defn wait-for-attach [endpoint instance-id timeout]
  (wait-for {:timeout timeout} 
    #(= "attached" (instance-desc endpoint instance-id :block-device-mappings 0 :ebs :status)) 
    {:type ::aws:ebs-attach-failed} "Failed to wait for ebs root device attach"))

(defn volume-desc [endpoint volume-id & ks]
  (-> 
   (with-ctx ec2/describe-volumes {:volume-ids [volume-id]})
    :volumes first (get-in ks)))

; TODO add : standard, io1, gp2 volume-type
(defn handle-volumes 
   "attaches and waits for ebs volumes" 
   [{:keys [aws machine] :as spec} endpoint instance-id]
  (when (= (image-desc endpoint (image-id machine) :root-device-type) "ebs")
    (wait-for-attach endpoint instance-id [10 :minute]))
  (let [zone (instance-desc endpoint instance-id :placement :availability-zone)]
    (doseq [{:keys [device size volume-type]} (aws :volumes)
            :let [v {:size size :availability-zone zone :volume-type volume-type}
                  {:keys [volume]} (with-ctx ec2/create-volume v) 
                  {:keys [volume-id]} volume]]
        (wait-for {:timeout [10 :minute]} 
           #(= "available" (volume-desc endpoint volume-id :state))
           {:type ::aws:ebs-volume-availability} "Failed to wait for ebs volume to become available")
        (with-ctx ec2/attach-volume 
          {:volume-id volume-id :instance-id instance-id :device device})
        (wait-for {:timeout [10 :minute]} 
           #(= "attached" (volume-desc endpoint volume-id :attachments 0 :state))
           {:type ::aws:ebs-volume-attach-failed} "Failed to wait for ebs volume device attach")
        (debug "attached volume" volume-id "owened by" instance-id)
        )))

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
