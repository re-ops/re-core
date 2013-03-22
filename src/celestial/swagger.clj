(ns celestial.swagger 
 (:use 
   clojure.pprint
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

(def resource-listing-data (atom (struct resource-listing)))

(def apis (atom {}))

(defmacro GET- "Generate a swagger enabled GET route."
  [path args desc & body]
  `(do 
     (with-meta (GET ~path ~args ~@body) 
        (merge (struct operation) ~desc {:httpMethod "GET" :path ~path}))))

(defmacro POST- "Generate a swagger enabled POST route."
  [path args desc & body]
  `(with-meta (POST ~path ~args ~@body) 
        (merge (struct operation) ~desc {:httpMethod "POST" :path ~path})))

(defn create-api [name & routes]
   (let [_apis (filter identity (mapv meta (rest routes)))]
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
  (GET- "/machine/" [^String host] 
    {:nickname "getMachine" :summary "gets a machine"}  
      (println host))
  (POST "/machine/" [^String host] 
    {:nickname "setFoo" :summary "sets a machine"}  
      (println host))))

(pprint @apis)

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

