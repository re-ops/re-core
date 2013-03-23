(ns celestial.swagger 
 "Swagger integration for Compojure, see https://github.com/wordnik/swagger-core/wiki/Resource-Listing for api definitions."
 (:refer-clojure :exclude [replace])
 (:use 
   clojure.pprint
   [flatland.useful.seq :only (find-first)]
   [clojure.string :only (replace)]
   [clojure.core.strint :only (<<)]
   [compojure.core :only (defroutes GET POST context)])
   (:require 
     [compojure.route :as route])) 

(def base "http://localhost:8082/")

(defstruct base-swag :apiVersion :swaggerVersion :basepath)

(defstruct resource-listing :apiVersion :swaggerVersion :basepath :apis)

(defstruct api-decleration  :apiVersion :swaggerVersion :basepath :resourcePath :apis :models)

(defstruct bare-api :path :description)

(defstruct api :path :description :operations)

(defstruct operation :httpMethod :nickname :responseClass :parameters :summary :notes :errorResponses)

(defstruct parameter :paramType :name :description :dataType :required :allowableValues :allowMultiple)

(def ^{:doc "see https://github.com/wordnik/swagger-core/wiki/Datatypes"}
  primitives #{:byte :boolean :int :long :float :double :string :Date})

(def resource-listing-data (atom (struct resource-listing)))

(def apis (atom {}))

(defn guess-type [path arg]
  (let [m (meta arg)]
    {:paramType (or (m :paramType) (if (.contains path (<< ":~(str arg)")) "path" "body"))
     :dataType (name (or (find-first primitives (keys m)) (m :dataType)))
     }))


(defn create-params [path args] 
  (let [defaults (struct parameter nil nil nil "String" true nil false) ]
    (mapv 
      #(-> % meta 
           (merge defaults {:name (str %)} (guess-type path %))) args)))

(defn create-op [desc verb params]
  (merge (struct operation) desc {:parameters params :httpMethod verb}))

(defn swag-path [path]
  "Converts compojure params to swagger params notation"
   (replace path #"\:(\w*)" (fn [v] (<< "{~{(second v)}}"))))

(defmacro swag-verb 
  "Creates a swagger enabled http route with a given verb"
  [verb path args desc & body]
  `(with-meta (~verb ~path ~args ~@body) 
      (struct api ~(swag-path path) "THis should be taken from defroute!"
           [(create-op ~desc (-> ~verb var meta :name) ~(create-params path args))])))

(defmacro GET- 
  "A swagger enabled GET route."
  [path args desc & body]
  `(swag-verb GET ~path ~args ~desc ~@body))

(defmacro POST- 
  "A swagger enabled POST route."
  [path args desc & body]
  `(swag-verb POST ~path ~args ~desc ~@body))

(defn combine-apis [_apis]
  "Merges api routes with same paths different verbs"
  (mapv #(apply merge-with (fn [f s] (if (vector? f) (into [] (concat f s)) f)) %)
        (vals (group-by :path _apis))))

(defn create-api [name & routes]
  (let [_apis (filterv identity (map meta (rest routes)))]
    (swap! apis assoc (keyword name) 
           (struct api-decleration "0.1" "1.1" (<< "~{base}api") (<< "/~{name}") (combine-apis _apis) []))))

(defmacro defroutes-
  "A swagger enabled defroute"
  [name & routes]
  `(do 
     (create-api ~(str name) ~@routes)
     (defroutes ~name ~@routes)))

#_(defroutes- machines {:path "/machine" :description "Operations on machines"}
     (GET- "/machine/:host" [^:string host] 
        {:nickname "getMachine" :summary "gets a machine"}  ())
     (POST- "/machine/:host" [^:string host] 
           {:nickname "addMachine" :summary "adds a machine"} ())
     (GET- "/machine/:cpu" [^:int cpus] 
           {:nickname "getCpus" :summary "get machines cpus list"} ())  
   )

(def celetial-listing
  (struct resource-listing "0.1" "1.1" base 
          [(struct bare-api "/api/jobs" "Job scheduling operations")
           (struct bare-api "/api/hosts" "Hosts operations")]))

(defroutes api-declerations
  (GET "/hosts" [] {:body (@apis :hosts)})
  (GET "/jobs" [] {:body (@apis :jobs)}))

(defroutes swagger-routes
  (context "/api" [] api-declerations)
  (GET "/api-docs.json" [h] {:body celetial-listing})
  (route/files "/swagger/" {:root (str (System/getProperty "user.dir") "/public/swagger-ui-1.1.7/")}))

