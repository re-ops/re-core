(ns supernal.core
  "A remote task execution framework (similar to fabric and capistrano) with follwing features:

  * Basic capistrano like functionality
  * A central server for running tasks
  * Clear seperation between tasks and execution/dependency order
  * Dynamic role and host lookup for tasks invocation targets
  * First class (java) package deployment lifecycle
  * can be used as a library and as a standalone tool  
  * Zeromq agent for improved perforemance over basic ssh
  "
  (:use (clojure.pprint))
 )

(def spaces (atom {}))

(defn task [ns* name* body]
  (list 'intern (list symbol ns*) (list symbol name*) (concat '(fn [])  body)))

(defmacro ns- [ns* & tasks]
  (let [gen-ns (gensym ns*)]
    `(do 
       (swap! spaces assoc (keyword '~ns*) '~gen-ns)
       (create-ns '~gen-ns)
       ~@(map 
           (fn [[_ name* & body]]
             (task (str gen-ns) (str name*) body)) tasks)))) 

(defn run [cmd] )

(defn copy [src dst])

(defmacro execute [ns* args role]
   
  )

(defmacro lifecycle [ns* plan]
    
  )

(lifecycle deploy 
  {update-code #{stop}
   release #{update-code}
   stop #{}
   start #{}})


(ns- deploy 
     (task update-code
           (let [war-url "http://..."]
             (copy war-url "/tmp"))) 

     (task start 
           (println "stoping service")
           (run "sudo service tomcat start")) 

     (task stop
           (run "sudo service tomcat stop")) ) 

(execute deploy {:war "foo"} :bar)
