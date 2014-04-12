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

(ns docker.client
  "Docker client"
  (:require 
    [clojure.core.strint :refer (<<)]
    [flatland.useful.seq :refer [find-first]]
    [clojure.string :as s]
    [docker.remote :as r]))

(defn camel-to-dash
  [s]
  (apply str 
    (map #(if (Character/isUpperCase ^Character %) (str "-" (s/lower-case %)) %) s)))

(def defaults
  {:hostname "" :user "" :attach-stdin false :attach-stdout true
   :attach-stderr true :port-specs nil :tty false
   :open-stdin false :stdin-once false :env nil
   :dns nil :volumes {} :volumes-from ""
   :working-dir "" :exposed-ports {}})

(defn create 
   "Create a new container instance" 
   [node spec]
  {:pre [(every? (partial contains? spec) [:image :memory :exposed-ports :volumes] )]}
  (r/docker-post node "containers/create" (merge defaults spec)))

(defn to-params
   "converts a map into a url params" 
   [m]
  (s/join "&" (map (fn [[k v]] (<< "~(name k)=~(str v)")) m)))

(defn start 
   "Start a new container instance" 
   [node id m]
  (r/docker-post node (<< "containers/~{id}/start") m))

(defn stop
   "Start a new container instance" 
  ([node id] (stop node id 5))
  ([node id timeout] (r/docker-post node (<< "containers/~{id}/stop?t~{timeout}") {})))

(defn commit 
  "Commit a container into an image" 
  [node id m]
  {:pre [(every? (partial contains? m) [:m :repo])]}
  (let [args (update-in (assoc m :container id) [:config] r/camelize)]
    (r/docker-post node (<< "commit?~(to-params args)") {})))

(defn delete 
  "Delete a container (optionaly clearing volumes)" 
  ([node id] (delete node id true))
  ([node id clear-vs] (r/docker-delete node (<< "containers/~{id}?v=~{clear-vs}"))))

(defn inspect 
   "retuns container status" 
   [node id]
  {:pre [id]}
   (r/docker-get node (<< "containers/~{id}/json")))

(defn find-image
  "find image id from tags, a tag has the form ubuntu:precise" 
  [node tag]
  (find-first (fn [{:keys [repo-tags]}] (find-first #{tag} repo-tags))
    (r/docker-get node "images/json?all=0")))

(defn list-containers 
  "list all containers" 
  [node m]
  (r/docker-get node (<< "containers/json?~(to-params m)")))

(comment 
  (defmacro trace-error [call]
   `(try 
      ~call 
     (catch Throwable e# (println e#))) 
    )

  (def id "14f6cd07873bdadb84bb8ddd86f73d89ce3be3759ffec6194fb59e748afe6579")

  (list-containers :local {:all false})

  {:id "14f6cd07873bdadb84bb8ddd86f73d89ce3be3759ffec6194fb59e748afe6579", :warnings ["Your kernel does not support memory swap capabilities. Limitation discarded."]}

  (trace-error
    (start :local id 
     {:binds ["/tmp:/tmp"] 
      :port-bindings {"22/tcp" [{ :host-port "2222" }]}}))

  (stop :local id)
  (delete :local id)

  (commit :local id 
     {:m "latest changes" :tag "v1" :author "ronen" :repo "narkisr" 
      :config {:cmd ["cat" "/world"] :port-specs ["22"]}})

  (find-image :local "narkisr/latest")
  (clojure.pprint/pprint (:state (inspect :local id))) 
  ) 


