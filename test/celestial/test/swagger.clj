(ns celestial.test.swagger
  (:use 
    clojure.test 
    [compojure.core :only (POST)] 
    [celestial.swagger :only (defroutes- GET- POST- apis)])   
 )

(defn swag-meta [r & ks] (-> r meta (get-in ks)))

(deftest half-way-doc 
  (defroutes- machines {:path "/machines" :description "Operations on machines"}
    (GET- "/machine/" [^:string host] {:nickname "getMachine" :summary "gets a machine"}  
          (println host))
    (POST "/machine/" [^:string host] (println host)))
  (is (= (count (get-in @apis [:machines :apis])) 1)))

(deftest manual-params 
  (defroutes- machines {}
    (GET- "/machine/" 
      [^{:paramType "query" :dataType "String"} host] {:nickname "getMachine" :summary "gets a machine"}  
          ()))
  (let [param (get-in @apis [:machines :apis 0 :parameters 0 ])]
    (is (= (param :dataType) "String")) 
    (is (= (param :paramType) "query"))) 
  )

(deftest auto-param-type-guessing 
  (is (= (swag-meta 
           (GET- "/machine/" [^:string host] {} ()) :parameters 0 :dataType) "string"))
  (is (= (swag-meta 
           (GET- "/machine/:host" [^:string host] {} ()) :parameters 0 :paramType) "path"))
  )
