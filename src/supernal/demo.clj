(ns supernal.demo
  (:use 
    clojure.pprint
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (lifecycle ns- execute run copy env)]))


(env 
  {:roles 
   {:web #{ {:host "localhost" :user "ronen"}
            {:host "192.168.5.9" :user "vagrant"}}}})

(ns- deploy 
  (task update-code
     (let [war-url "http://..."]
       (debug "updating code on" remote)
       #_(copy war-url "/tmp"))) 

  (task start 
    (debug "starting service on" remote)
    (run "echo 'foo'")) 

  (task stop)) 

(ns- deploy 
  (task stop
    (debug "stopping service" remote)
    (run "hostname")))

(ns- bar 
    (task play))

(lifecycle deploy 
  {update-code #{start}
   stop #{update-code}
   start #{}})

; (println deploy-lifecycle)
(execute deploy {:war "foo"} :web)

;(execute deploy/stop) 
