(ns supernal.demo
  (:use 
    clojure.pprint
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (lifecycle ns- execute run copy env)]))


(env 
  {:roles 
    {:web #{{:host "localhost" :user "ronen"}
            {:host "192.168.5.9" :user "vagrant"}}}})

(ns- deploy 
  (task update-code
       (debug "updating code on" remote)
       (copy "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz" "/tmp")) 

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
