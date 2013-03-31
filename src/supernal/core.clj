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
  (:require [supernal.sshj :as sshj]) 
  (:use 
    [celestial.topsort :only (kahn-sort)] 
    (clojure.pprint)))


(defn gen-ns [ns*]
  (symbol (str "supernal.user." ns*)))

(defn apply-env 
  "Tasks wrapping functions (run or copy) and applies env to them (calling run- copy-)."
  [body]
  (let [shim #{'run 'copy}] 
    (map #(if (shim (first %)) (list % 'remote) %) body)))

(defn task [ns* name* body]
  (list 'intern (list symbol ns*) (list symbol name*) (concat '(fn [args remote]) (apply-env body))))

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

(defn run-cycle [cycle* args remote]
   (doseq [t cycle*]
     (t args remote)))

(defmacro execute [ns* args role]
  "Excutes the lifecycle of a given ns"
  `(doseq [remote# (get-in @~'env- [:roles ~role])] 
     (future (run-cycle ~(gen-lifecycle ns*) ~args remote#))))

(defmacro env 
  "A hash of running enviroment info and roles"
  [hash*]
  `(intern *ns* (symbol  "env-") (atom ~hash*)))

(defn role->hosts 
  "A hash based role to hosts resolver"
  [role] 
  )

(defn run
  "Running a given cmd on a remote system" 
  [cmd]
  (partial sshj/execute cmd))


(defn copy
  "Copies src uri (either http/file/git) into a remote destination path" 
  [src dst]
  (partial sshj/copy src dst))


