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
