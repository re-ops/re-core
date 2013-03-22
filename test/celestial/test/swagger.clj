(ns celestial.test.swagger
  (:use 
    clojure.test 
    [compojure.core :only (POST)] 
    [celestial.swagger :only (defroutes- GET- POST- apis)])   
 )


(deftest half-way-doc 
  (defroutes- machines {:path "/machines" :description "Operations on machines"}
    (GET- "/machine/" [^String host] {:nickname "getMachine" :summary "gets a machine"}  
          (println host))
    (POST "/machine/" [^String host] (println host)))
  (is (= (count (get-in @apis [:machines :apis])) 1)))

(deftest parameters 
   (defroutes- machines {:path "/machines" :description "Operations on machines"}
    (GET- "/machine/" [^{:paramType "body" :dataType "String"} host] {:nickname "getMachine" :summary "gets a machine"}  
          (println host))
    (POST "/machine/" [host] (println host)))
    (is (= (get-in @apis [:machines :apis 0 :parameters 0 :dataType]) "String"))
  )
