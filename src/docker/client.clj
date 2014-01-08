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
 (create :local {:image "b36f06432104" :memory 0 :args ["-d" "-n"]})
 (stop :local "032f7071997a286f1caadce4f8915cf9982df7c42d9c9a36eb1209250d340242")
 (find-image :local "narkisr/sshd:latest")) 

