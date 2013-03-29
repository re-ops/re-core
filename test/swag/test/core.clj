(ns swag.test.core
  (:use 
    clojure.test 
    [compojure.core :only (POST)] 
    [swag.core :only (defroutes- GET- POST- apis)]
    [swag.model :only (defmodel models)] 
    ))

(defn swag-meta [r & ks] (-> r meta (get-in ks)))

(deftest half-way-doc 
  (defroutes- machines {:path "/machines" :description "Operations on machines"}
    (GET- "/machine/" [^:string host] {:nickname "getMachine" :summary "gets a machine"}  
          (println host))
    (POST "/machine/" [^:string host] (println host)))
  (is (= (count (get-in @apis [:machines :apis])) 1)))

(deftest manual-params 
  (defroutes- machines {}
    (GET- "/machine/" [^{:paramType "query" :dataType "String"} host] {:nickname "getMachine" :summary "gets a machine"}  
          ()))
  (let [param (get-in @apis [:machines :apis 0  :operations 0 :parameters 0])]
    (is (= (param :dataType) "String")) 
    (is (= (param :paramType) "query"))) 
  )

(deftest auto-param-type-guessing 
  (let [param (swag-meta (GET- "/machine/:host" [^:string host] {} ()) :operations 0 :parameters 0)]
    (is (= (param :dataType)  "string")) 
    (is (= (param :paramType) "path"))) 
  )

(defmodel type :id :string)

(deftest using-model 
  (let [param (swag-meta (GET- "/machine/" [^:string host ^:type type] {} ()) :operations 0 :parameters 1)]
    (is (= (param :dataType)  "Type")) 
    (is (= (param :paramType) "body"))))
