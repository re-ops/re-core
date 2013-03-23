(ns celestial.swagger 
 "Swagger integration for Compojure, see https://github.com/wordnik/swagger-core/wiki/Resource-Listing for api definitions."
 (:use 
   clojure.pprint
   [flatland.useful.seq :only (find-first)]
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

(defmacro swag-verb 
  "Creates a swagger enabled http route with a given verb"
  [verb path args desc & body]
  `(with-meta (~verb ~path ~args ~@body) 
              (merge (struct operation) ~desc 
                     {:parameters ~(create-params path args) :httpMethod (-> ~verb var meta :name) :path ~path})))

(defmacro GET- 
  "A swagger enabled GET route."
  [path args desc & body]
  `(swag-verb GET ~path ~args ~desc ~@body))

(defmacro POST- 
  "A swagger enabled POST route."
  [path args desc & body]
  `(swag-verb POST ~path ~args ~desc ~@body))

(defn create-api [name & routes]
  (let [_apis (filterv identity (map meta (rest routes)))]
    (swap! apis assoc (keyword name) 
           (struct api-decleration "0.1" "1.1" (<< "~{base}api") (<< "/~{name}") _apis []))))

(defmacro defroutes-
  "A swagger enabled defroute"
  [name & routes]
  `(do 
     (create-api ~(str name) ~@routes)
     (defroutes ~name ~@routes)))

(macroexpand 
  '(defroutes- machines {:path "/machines" :description "Operations on machines"}
     (GET- "/machine/" [^{:paramType "body" :dataType "String"} host] 
           {:nickname "getMachine" :summary "gets a machine"}  
           (println host))
     (POST "/machine/" [^String host] 
           {:nickname "setFoo" :summary "sets a machine"}  
           (println host))))

(def celetial-listing
  (struct resource-listing "0.1" "1.1" base 
          [(struct bare-api "/api/registry" "Registry operations") 
           (struct bare-api "/api/machine" "Machine operations")]))

(defroutes api-declerations
  (GET "/registry" []  {}
       {:body 
        (struct api-decleration
                "0.1" "1.1" "http://localhost:8082/api" "/registry" 
                [(struct api "/registry/host/machine/{name}" "Getting machine"
                         [(struct operation "GET" "getHost" "" 
                                  [(struct parameter "path" "name" "machine hostname" "String" true nil false)]
                                  "Getting host" "Use with care" "")])] {})}) 
  (GET "/machine" [] {}
       {:body 
        {:apiVersion "0.1",
         :swaggerVersion "1.1",
         :basePath "http://localhost:8082/api",
         :resourcePath "/machine"
         :apis []
         :models {}}})
  )

(defroutes swagger-routes
  (context "/api" [] api-declerations)
  (GET "/api-docs.json" [h] {:body celetial-listing})
  (route/files "/swagger/" {:root (str (System/getProperty "user.dir") "/public/swagger-ui-1.1.7/")}))

