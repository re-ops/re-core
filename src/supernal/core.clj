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
  (:use 
    [celestial.topsort :only (kahn-sort)] 
    (clojure.pprint)))


(defn gen-ns [ns*]
  (symbol (str "supernal.user." ns*)))

(defn task [ns* name* body]
  (list 'intern (list symbol ns*) (list symbol name*) (concat '(fn [args])  body)))

(defmacro ns- 
  "Tasks ns macro, a group of tasks is associated with matching functions under the supernal.user ns"
  [ns* & tasks]
  `(do 
     (create-ns '~(gen-ns ns*))
     ~@(map 
         (fn [[_ name* & body]]
           (task (str (gen-ns ns*)) (str name*) body)) tasks))) 

(defn gen-lifecycle [ns*]
  (symbol (str ns* "-lifecycle")))

(defmacro lifecycle 
  "Generates a topological sort from a lifecycle plan"
  [ns* plan]
  `(def ~(gen-lifecycle ns*)
     (kahn-sort (reduce (fn [r# [k# v#]] 
                          (assoc r# (ns-resolve '~(gen-ns ns*) k#) 
                                 (into #{} (map #(ns-resolve '~(gen-ns ns*) %) v#)))) {} '~plan))))

(defmacro execute [ns* args role]
  "Excutes the lifecycle of a given ns"
  `(doseq [t# ~(gen-lifecycle ns*)] (t# ~args)))

(defn role->hosts [role]

  )

(defn run [cmd])

(defn copy [src dst])

