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
   :working-dir "" :exposed-ports {}})

(defn create 
   "Create a new container instance" 
   [node spec]
  {:pre [(every? (partial contains? spec) [:image :memory] )]}
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

  (def id "1901ac2149b30bc39c3e7357455bb8929318e67ba4f7e09ffb51ed3adde2480e")

  (list-containers :local {:all false})

  (create :local {:image "narkisr:latest" :memory 0 :args ["-d" "-n"]})

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
  (clojure.pprint/pprint (:config (inspect :local ))) 
  ) 


