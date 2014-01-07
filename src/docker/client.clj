(ns docker.client
  "Docker client"
  (:require 
    [clojure.string :as s]
    [docker.remote :as r])
 )

(defn camel-to-dash
  [s]
  (apply str 
    (map #(if (Character/isUpperCase %) (str "-" (s/lower-case %)) %) s)))

(def defaults
  {:hostname "" :user "" :memory-swap 0
    :attach-stdin false :attach-stdout true
    :attach-stderr true :port-specs nil :tty false
    :open-stdin false :stdin-once false :env nil
    :cmd ["/bin/bash"] :dns nil 
    :volumes { "/tmp"  {} } :volumes-from ""
    :working-dir "" :exposed-ports { "22/tcp" {}}})

(defn create 
   "Create a new container instance" 
   [node spec]
  {:pre [(every? (partial contains? spec) [:image :memory] )]}
  (r/docker-post node "containers/create" (merge defaults spec)))



(comment 
  (celestial.model/set-env :dev 
  (try 
    (create :local {:image "8dbd9e392a96" :memory 0})
   (catch Throwable e
     (clojure.pprint/pprint e)  
     ) 
    ))) 
  
