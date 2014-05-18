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

(ns celestial.api.jobs
  "Jobs manangement API"
  (:refer-clojure :exclude [hash type])
  (:require 
    [cemerick.friend :as friend]
    [flatland.useful.map :refer  (map-vals)]
    [swag.model :refer (defmodel)]
    [clojure.core.strint :refer (<<)]
    [celestial.common :refer (bad-req success import-logging get* link)]
    [gelfino.timbre :refer (get-tid)]
    [celestial.persistency [types :as t] [users :as u]]
    [celestial.persistency.actions :refer (find-action-for)]
    [celestial.jobs :as jobs :refer (enqueue)]
    [es.jobs :as es]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes- errors)]
    [celestial.security :refer (current-user)]
    [celestial.persistency.systems :as s]))

(import-logging)

(defmodel hash)

(defn add-tid-link [{:keys [tid] :as job}]
  (assoc job :tid-link (link "query=tid:~{tid}&fields=@timestamp,message,tid," {:tid tid})))

(defn schedule-job 
  ([^String id action msg]
    (schedule-job id action msg [(assoc (s/get-system id) :system-id (Integer. id))])) 
  ([id action msg args]
    (cond 
      (not (s/system-exists? id)) 
        (bad-req {:message (<< "No system found with given id ~{id}")})
      (not (u/op-allowed? action (current-user)))
        (bad-req {:message (<< "Operation ~{action} not allowed for user ~(current-user)")})
      :else
       (let [m {:identity id :args args :tid (get-tid) 
               :env (s/get-system id :env) :user (current-user)}]
        (success {:message msg :id id :job (enqueue action m)})))))

(defroutes- jobs {:path "/jobs" :description "Async job scheduling"}

  (POST- "/jobs/stage/:id" [^:int id] 
    {:nickname "stageSystem" :summary "Complete end to end staging job"
     :notes "Combined system creation and provisioning, separate actions are available also."}
      (let [system (s/get-system id) type (t/get-type (:type system))]
           (schedule-job id "stage" "submitted system staging" [type (assoc system :system-id (Integer. id))])))

  (POST- "/jobs/create/:id" [^:int id] 
     {:nickname "createSystem" :summary "System creation job"
      :errorResponses (errors {:bad-req "Missing system"})
      :notes "Creates a new system on remote hypervisor (usually followed by provisioning)."}
         (schedule-job id "create" "submitted system creation"))

  (POST- "/jobs/reload/:id" [^:int id] 
     {:nickname "reloadSystem" :summary "System reload job"
      :errorResponses (errors {:bad-req "Missing system"})
      :notes "Reloads a system by destroying the VM and then re-creating it"}
         (schedule-job id "reload" "submitted system reloading"))

  (POST- "/jobs/destroy/:id" [^:int id] 
    {:nickname "destroySystem" :summary "System destruction job"
     :notes "Destroys a system, clearing it both from Celestial's model storage and hypervisor"}
         (schedule-job id "destroy" "submitted system destruction"))

  (POST- "/jobs/clear/:id" [^:int id] 
    {:nickname "clearSystem" :summary "System clearence job"
     :notes "Clears a system, only clearing it both from Celestial's model storage not the hypervisor"}
         (schedule-job id "clear" "submitted system clearence"))

  (POST- "/jobs/start/:id" [^:int id] 
    {:nickname "startSystem" :summary "System start job" :notes "Starts a system"}
         (schedule-job id "start" "submitted system start"))

  (POST- "/jobs/stop/:id" [^:int id] 
    {:nickname "stopSystem" :summary "System stop job" :notes "Stops a system"}
         (schedule-job id "stop" "submitted system stop"))

  (POST- "/jobs/provision/:id" [^:int id] 
    {:nickname "provisionSystem" :summary "Provisioning job"
     :notes "Starts a provisioning workflow on a remote system
             using the provisioner configured in system type"}
         (let [system (s/get-system id) type (t/get-type (:type system))]
           (schedule-job id "provision" "submitted provisioning" 
              [type (assoc system :system-id (Integer. ^String id))])))

  (POST- "/jobs/clone/:id" [^:int id & ^:hash clone-spec] 
     {:nickname "cloneSystem" :summary "Clones a system" 
      :notes "Clones a system by copying its model and replacing unique identifiers."}
      (schedule-job id "clone" (<< "submitted cloning") [id clone-spec]))

  (POST- "/jobs/:action/:id" [^:string action ^:int id & ^:hash args] 
     {:nickname "runAction" :summary "Run remote action" 
      :notes "Runs adhoc remote opertions on system (like deployment, service restart etc)
              using matching remoting capable tool like Capisrano/Supernal/Fabric"}
       (let [{:keys [machine] :as system} (s/get-system id)]
         (if-let [action* (find-action-for action (:type system))]
           (schedule-job id "run-action" (<< "submitted ~{action} action") 
             [action* (merge args {:action (keyword action) :hostname (machine :hostname) :target (machine :ip) :system-id (Integer. ^String id)})])
           (bad-req {:message (<< "No action ~{action} found for id ~{id}")})
           )))

  (GET- "/jobs/:queue/:uuid/status" [^:string queue ^:string uuid]
        {:nickname "jobStatus" :summary "single job status tracking" 
         :notes "job status can be pending, processing, done or nil"}
        (success {:job (jobs/status queue uuid)}))
    
  (GET- "/jobs/runnning" []
        {:nickname "jobsStatus" :summary "Global job status tracking" 
         :notes "job status can be either pending, processing, done or nil"}
        (let [{:keys [username]} (friend/current-authentication)
              {:keys [envs] :as user} (u/get-user username)]
          (success (map-vals (jobs/jobs-status envs) (partial map add-tid-link)))))
  
  (GET- "/jobs/done" [^:int page ^:int offset]
      {:nickname "getDoneJobs" :summary "Get done jobs"}
     (let [page* (Integer/valueOf page) offset* (Integer/valueOf offset)
        {:keys [username]} (friend/current-authentication)
        {:keys [envs] :as user} (u/get-user username)]
       (success (es/paginate (* (- page* 1) offset*) (* page*  offset*) envs))))
  )
