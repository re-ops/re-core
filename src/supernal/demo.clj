(ns supernal.demo
  (:use 
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (lifecycle ns- execute run copy env)]))


(env 
  {:roles 
    {:web #{{:host "localhost" :user "ronen"}
            {:host "192.168.5.9" :user "vagrant"}}}})

(ns- deploy 
  (task stop
    (debug "stopping service" remote)
    (run "hostname")))

(ns- bar 
    (task stop2
     (let [foo 1]
       (debug "stoping in bar!"))))

(lifecycle basic-deploy
  {deploy/update-code #{deploy/start}
   deploy/stop #{deploy/update-code bar/stop2}
   bar/stop2 #{}
   deploy/start #{}})

(def artifact "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz")

(execute basic-deploy {:app-name "foo" :src artifact} :web)

(execute deploy/stop {:app-name "foo" :src artifact} :web ) 
