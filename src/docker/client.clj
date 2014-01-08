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
    (map #(if (Character/isUpperCase %) (str "-" (s/lower-case %)) %) s)))

(def defaults
  {:hostname "" :user "" :attach-stdin false :attach-stdout true
   :attach-stderr true :port-specs nil :tty false
   :open-stdin false :stdin-once false :env nil
   :dns nil :volumes {} :volumes-from ""
   :working-dir "" :exposed-ports { "22/tcp" {}}})

(defn create 
   "Create a new container instance" 
   [node spec]
  {:pre [(every? (partial contains? spec) [:image :memory] )]}
  (r/docker-post node "containers/create" (merge defaults spec)))

(defn start 
   "Start a new container instance" 
   [node id]
  (r/docker-post node (<< "containers/~{id}/start") {}))

(defn stop
   "Start a new container instance" 
  ([node id] (stop node id 5))
  ([node id timeout] (r/docker-post node (<< "containers/~{id}/stop?t~{timeout}") {})))

(defn to-params
   "converts a map into a url params" 
   [m]
  (reduce (fn [r [k v]] (<< "~{r}&~(name k)=~(str v)")) "" 
    (update-in m [:config] r/camelize)))

(defn commit 
  "Commit a container into an image" 
  {:pre [(every? (partial contains? spec) [:image :memory] )]}
  [node id m]
  (r/docker-post node (<< "commit?container=~{id}~(to-params m)") {}))

(defn delete 
  "Delete a container" 
  [node id]
  (r/docker-delete node "containers/~{id}?v=1"))

(defn find-image
  "find image id from tags, a tag has the form ubuntu:precise" 
  [node tag]
  (find-first (fn [{:keys [repo-tags]}] (find-first #{tag} repo-tags))
              (r/docker-get node "images/json?all=0")))

(defn list-containers 
  "list all containers" 
  [node]
  (r/docker-get node "containers/json?all=0"))


(comment 
  (-> (list-containers :local) first :id)
  (start :local "0870ab0ded8a260cd18021151399a58c7b65e1351e83348b544aa05f623c1307")
  (commit :local "0870ab0ded8a260cd18021151399a58c7b65e1351e83348b544aa05f623c1307" 
     {:m "latest changes" :tag "v1" :author "ronen" :repo "narkisr" 
      :config {:cmd ["cat" "/world"] :port-specs ["22"]}})
  (create :local {:image "b36f06432104" :memory 0 :args ["-d" "-n"]})
  (stop :local "")
  (delete :local "")
  (find-image :local "narkisr/sshd:latest")) 


