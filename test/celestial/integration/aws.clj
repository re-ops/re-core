(ns aws.provider
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:use 
     celestial.integration.aws clojure.test
     [celestial.fixtures :only (redis-ec2-spec)]
        ))


(deftest ^:ec2 full-cycle 
    (let [instance (vconstruct redis-ec2-spec)]
      (.stop instance)
      (.delete instance) 
      (.create instance) 
      (.start instance)
      (is (= (.status instance) "running"))))
