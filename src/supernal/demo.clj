(ns supernal.demo
  (:use [supernal.core :only (lifecycle ns- execute run copy)])  
  )


(ns- deploy 
  (task update-code
     (let [war-url "http://..."]
       (copy war-url "/tmp"))) 

  (task start 
    (println "starting service")
    (run "sudo service tomcat start")) 

  (task stop
    (run "sudo service tomcat stop")) ) 

(ns- deploy 
  (task stop
    (println "stopping service" args)
    (run "sudo service tomcat stop")) )

(lifecycle deploy 
  {update-code #{start}
   stop #{update-code}
   start #{}})

;(execute deploy {:war "foo"} :bar)

;(execute deploy/stop) 
